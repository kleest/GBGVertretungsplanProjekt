/*
 * Copyright (c) 2014 Steffen Klee
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.stkl.gbgvertretungsplan.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.os.Bundle;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import de.stkl.gbgvertretungsplan.Util;
import de.stkl.gbgvertretungsplan.errorreporting.ErrorReporter;
import de.stkl.gbgvertretungsplan.priv.GBGCommunication;
import de.stkl.gbgvertretungsplan.values.Sync;
import de.stkl.gbgvertretungsplan.networkcommunication.CommunicationInterface;


/**
 * Created by Steffen Klee on 10.12.13.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private final String LOG_TAG = "SyncAdapter";
    private final AccountManager mAccountManager;
    private final Context mContext;

    private static CommunicationInterface mComInterface;


    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mAccountManager = AccountManager.get(context);
        mContext = context;
        if (mComInterface == null)
            mComInterface = GBGCommunication.getInstance();
     }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mAccountManager = AccountManager.get(context);
        mContext = context;
        if (mComInterface == null)
            mComInterface = GBGCommunication.getInstance();
    }

    public static CommunicationInterface getComInterface() {
        if (mComInterface == null)
            mComInterface = GBGCommunication.getInstance();
        return mComInterface;
    }

    // this method performs the real sync!
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        //android.os.Debug.waitForDebugger();
        reportStatus(Sync.SYNC_STATUS.START);

        String username = account.name;
        String password = mAccountManager.getPassword(account);

        AndroidHttpClient httpClient = AndroidHttpClient.newInstance(null);

        CookieStore cookies = new BasicCookieStore();
        HttpContext localContext = new BasicHttpContext();
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookies);

        boolean error = false;

        try {
            if (!mComInterface.login(httpClient, localContext, username, password))
                throw new LoginException();

            // 4. request and save pages (today + tomorrow)
            requestAndSaveDay(httpClient, localContext, 0); // today
            requestAndSaveDay(httpClient, localContext, 1);  // and tomorrow

            if (!logout(httpClient, localContext))
                throw new CommunicationInterface.LogoutException();

        } catch (IOException e) {
            error = true;
        } catch (CommunicationInterface.CommunicationException e) {
            ErrorReporter.reportError(e, mContext);
            error = true;
        } catch (CommunicationInterface.ParsingException e) {
            ErrorReporter.reportError(e, mContext);
            error = true;
        } catch (LoginException e) {
            error = true;
        } catch (CommunicationInterface.LogoutException e) {
            ErrorReporter.reportError(e, mContext);
            error = true;
        // generic exceptions like NullPointerException should also indicate a failed Sync
        } catch (Exception e) {
            error = true;
        } finally {
            if (error)
                reportStatus(Sync.SYNC_STATUS.ERROR);
            else
                reportStatus(Sync.SYNC_STATUS.OK);
            httpClient.close();
        }
    }

    private void reportStatus(Sync.SYNC_STATUS status) {
        Intent i = new Intent(Sync.ACTION_SYNC_FINISHED);
        i.putExtra("status", status);
        mContext.sendBroadcast(i);
    }

    private List<String> parseCategories(Element root) {
        // get table
        //Log.d(LOG_TAG, root.toString());
        Element table = root.select("table.mon_list").first();
        // category headlines
        List<String> categories = new ArrayList<String>();
        for (Element headline : table.select("tr:first-child th")) {
            categories.add(headline.text());
        }

        return categories;
    }

    private List<List<String>> parseRows(Element root) {
        Element table = root.select("table.mon_list").first();
        // each row has categories.size() categories, build a two dimensional array:
        // <row-index><category-index> = <value>
        // rows[0] is the name of the class, if multiple classes are set there, split them (separator: ,)
        List<List<String>> allRows = new ArrayList<List<String>>();
        Elements rows = table.select("tr:gt(0)");
        for (Element row : rows) {
            int i = 0;
            ArrayList<String> newrow = new ArrayList<String>();

            String[] pendingClasses = null;
            // each category
            for (Element categ: row.select("td")) {
                if (i == 0) {   // split class field by separator(,) if needed
                    String text = categ.text();
                    pendingClasses = text.split(",");
                }
                // dont add class if multiple classes are given
                if (i != 0 || (pendingClasses == null || pendingClasses.length == 0))
                    newrow.add(categ.text());
//                Log.i(LOG_TAG, categ.text());
                i++;
            }

            // add row with category info to allRows array, if not multiple classes
            if (pendingClasses == null || pendingClasses.length == 0)
                allRows.add(newrow);
            // otherwise set class names to multiple rows
            else {
                for (String classN : pendingClasses) {
                    ArrayList<String> n = (ArrayList<String>)newrow.clone();
                    n.add(0, classN.trim());
                    allRows.add(n);
                }
            }
        }

        return allRows;
    }

    private void requestAndSaveDay(HttpClient httpClient, HttpContext localContext, int day) throws IOException, CommunicationInterface.ParsingException, CommunicationInterface.CommunicationException {
        Element body = mComInterface.requestDay(httpClient, localContext, day);

        Map<String,String> generalData = parseGeneralData(body);
        List<String> categories = parseCategories(body);
        List<List<String>> rows = parseRows(body);

        if (categories.isEmpty() || rows.isEmpty())
            throw new CommunicationInterface.ParsingException();

        // save day to disk
        Storage.saveToDisk(mContext, generalData, categories, rows, day);
    }

    private Map<String,String> parseGeneralData(Element root) {
        Map<String,String> generalData = new HashMap<String, String>();
        // last update time and day
        Element updateTime = root.select("table.mon_head td:eq(2) p").first();
        if (updateTime != null) {
            Pattern pat = Pattern.compile("(Stand: [\\.:0-9 ]+)", Pattern.DOTALL);
            Matcher matcher = pat.matcher(updateTime.text());
            if (matcher.find())
                generalData.put(Sync.GENERAL_DATA_UPDATETIME, matcher.group(1));
        }
        // date the substitution table belongs to
        Element belongingDate = root.select("div.mon_title").first();
        if (belongingDate != null)
            generalData.put(Sync.GENERAL_DATA_DATE, belongingDate.text());

        // daily information
        Elements dailyInfos = root.select("table.info tr");
        int i=0;
        for (Element info: dailyInfos) {
            Elements e = info.select("td");
            if (e.size() != 2)
                continue;
            String title = e.first().text(), description = e.get(1).text();
            String keyTitle = "", keyDescription = "";
            switch(i) {
                case 0: keyTitle = Sync.GENERAL_DATA_DAILYINFO_1_TITLE; keyDescription = Sync.GENERAL_DATA_DAILYINFO_1_DESCRIPTION;
                    break;
                case 1: keyTitle = Sync.GENERAL_DATA_DAILYINFO_2_TITLE; keyDescription = Sync.GENERAL_DATA_DAILYINFO_2_DESCRIPTION;
                    break;
                case 2: keyTitle = Sync.GENERAL_DATA_DAILYINFO_3_TITLE; keyDescription = Sync.GENERAL_DATA_DAILYINFO_3_DESCRIPTION;
                    break;
                default:
                    break;
            }
            if (!keyTitle.equals("")) {
                generalData.put(keyTitle, title);
                generalData.put(keyDescription, description);
            }
            i++;
        }

        return generalData;
    }

    public static boolean tryLogin(String username, String password) throws CommunicationInterface.ParsingException, IOException, CommunicationInterface.CommunicationException {
        AndroidHttpClient httpClient = AndroidHttpClient.newInstance(null);

        CookieStore cookies = new BasicCookieStore();
        HttpContext localContext = new BasicHttpContext();
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookies);

        boolean result = tryLogin(httpClient, localContext, username, password);

        // cleanup
        httpClient.close();

        return result;
    }

    // logs out after success
    public static boolean tryLogin(HttpClient httpClient, HttpContext localContext, String username, String password) throws IOException, CommunicationInterface.CommunicationException, CommunicationInterface.ParsingException {
        if (getComInterface().login(httpClient, localContext, username, password)) {
            logout(httpClient, localContext);
            return true;
        } else
            return false;
    }

    public static boolean logout(HttpClient httpClient, HttpContext localContext) throws IOException, CommunicationInterface.CommunicationException, CommunicationInterface.ParsingException {
        return getComInterface().logout(httpClient, localContext);
    }
}
