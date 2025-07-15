/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.service.nextgen.query;

import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.service.nextgen.antlr.QueryLexer;
import org.jboss.sbomer.service.nextgen.antlr.QueryParser;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class EventsQueryProcessor {

    public EventsQueryListener process(String query) {
        QueryLexer lexer = new QueryLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        QueryParser parser = new QueryParser(tokens);

        parser.addErrorListener(new EventsQueryParseErrorListener());

        EventsQueryListener listener = new EventsQueryListener();
        ParseTreeWalker walker = new ParseTreeWalker();

        try {
            QueryParser.QueryContext tree = parser.query();
            walker.walk(listener, tree);


            if (tokens.LA(1) != org.antlr.v4.runtime.Token.EOF) {
                throw new ParseCancellationException(
                        "Invalid query syntax. The query could not be fully parsed. Check for errors near token: '"
                                + tokens.get(tokens.index()).getText() + "'");
            }
            log.info("Processed query: {}", query);

            return listener;

        } catch (ParseCancellationException | IllegalArgumentException | UnsupportedOperationException e) {

            throw new ClientException("Invalid query", List.of(e.getMessage()));
        } catch (Exception e) {

            log.error("Unexpected error while parsing query: {}", query, e);
            throw new ClientException("Invalid query", List.of("An unexpected error occurred while parsing the query"));
        }
    }
}
