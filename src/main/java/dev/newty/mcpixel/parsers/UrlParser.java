package dev.newty.mcpixel.parsers;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class UrlParser<C> implements ArgumentParser<C, URL> {
    public static <C> CommandComponent.@NonNull Builder<C, URL> component() {
        ParserDescriptor<C, URL> descriptor = ParserDescriptor.of(new UrlParser<>(), URL.class);
        return CommandComponent.<C, URL>builder().parser(descriptor);
    }

    @Override
    public @NonNull ArgumentParseResult<URL> parse(@NonNull CommandContext<C> ctx, @NonNull CommandInput input) {
        String token = input.readString();

        try {
            URI uri = new URI(token);

            if (uri.getScheme() == null) {
                return ArgumentParseResult.failure(
                        new IllegalArgumentException("URL is missing a scheme (e.g. http or https)")
                );
            }

            if (!uri.getScheme().equalsIgnoreCase("http") &&
                    !uri.getScheme().equalsIgnoreCase("https")) {
                return ArgumentParseResult.failure(
                        new IllegalArgumentException("Only http and https URLs are allowed")
                );
            }

            if (uri.getHost() == null) {
                return ArgumentParseResult.failure(
                        new IllegalArgumentException("URL is missing a valid host")
                );
            }

            return ArgumentParseResult.success(uri.toURL());

        } catch (URISyntaxException e) {
            return ArgumentParseResult.failure(
                    new IllegalArgumentException("Invalid URL format: " + e.getInput())
            );
        } catch (MalformedURLException e) {
            return ArgumentParseResult.failure(
                    new IllegalArgumentException("Malformed URL")
            );
        }
    }
}
