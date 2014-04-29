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

package de.stkl.gbgvertretungsplan.networkcommunication;

import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;

import org.jsoup.nodes.Element;

import java.io.IOException;

/**
 * Created by Steffen Klee on 03.03.14.
 */
public interface CommunicationInterface {
    public static final class CommunicationException extends Exception {
    }

    public static final class ParsingException extends Exception {
    }

    public static final class LogoutException extends Exception {
    }

    public boolean login(HttpClient httpClient, HttpContext localContext, String username, String password, int dataType) throws IOException, CommunicationException, ParsingException;
    public boolean logout(HttpClient httpClient, HttpContext localContext, int dataType) throws IOException, CommunicationException, ParsingException;
    public Element requestDay(HttpClient httpClient, HttpContext localContext, int index, int dataType) throws IOException, CommunicationException, ParsingException;
}
