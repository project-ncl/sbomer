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

        parser.removeErrorListeners(); // Remove the default console listener
        parser.addErrorListener(new EventsQueryParseErrorListener()); // Add our custom listener

        EventsQueryListener listener = new EventsQueryListener();
        ParseTreeWalker walker = new ParseTreeWalker();

        try {
            QueryParser.QueryContext tree = parser.query();
            walker.walk(listener, tree);

            // Check if the whole query was consumed
            if (tokens.LA(1) != org.antlr.v4.runtime.Token.EOF) {
                throw new ParseCancellationException(
                        "Invalid query syntax. The query could not be fully parsed. Check for errors near token: '"
                                + tokens.get(tokens.index()).getText() + "'");
            }

            return listener;

        } catch (ParseCancellationException | IllegalArgumentException | UnsupportedOperationException e) {
            // Catch specific, known exceptions and re-throw them as a ClientException
            throw new ClientException("Invalid query", List.of(e.getMessage()));
        } catch (Exception e) {
            // Catch any other unexpected errors
            log.error("Unexpected error while parsing query: {}", query, e);
            throw new ClientException("Invalid query", List.of("An unexpected error occurred while parsing the query"));
        }
    }
}
