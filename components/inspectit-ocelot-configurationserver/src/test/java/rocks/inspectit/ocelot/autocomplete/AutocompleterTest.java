package rocks.inspectit.ocelot.autocomplete;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class AutocompleterTest {

    Autocompleter completer;

    @BeforeEach
    void buildCompleter() {
        completer = new Autocompleter();
    }

    @Nested
    public class Parse {

        @Test
        void kebabCaseTest() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "iCan-parse-kebab", "case", "even-in-brackets\\wow", "thisIs-awesome"));
            assertThat(completer.parse("inspectit.iCan-parse-kebab.case[even-in-brackets\\wow].thisIs-awesome")).isEqualTo(output);
        }

        @Test
        void emptyString() {
            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.parse("")).isEqualTo(output);
        }

        @Test
        void nullString() {
            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.parse(null)).isEqualTo(output);

        }

        @Test
        void bracketAfterBracket() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "property", "first", "second"));
            assertThat(completer.parse("inspectit.property[first][second]")).isEqualTo(output);
        }

        @Test
        void dotInBrackets() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "property", "first.second"));
            assertThat(completer.parse("inspectit.property[first.second]")).isEqualTo(output);
        }

        @Test
        void throwsException() {
            try {
                completer.parse("inspectit.property[first.second");
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage().equals("invalid property path")).isEqualTo(true);
            }
        }

    }

    @Nested
    public class CheckPropertyName {
        @Test
        void correctProperty() {
            String property = "inspectit.service-name";
            assertThat(completer.checkPropertyName(property)).isEqualTo(true);
        }

        @Test
        void emptyString() {
            String property = "";
            assertThat(completer.checkPropertyName(property)).isEqualTo(false);
        }

        @Test
        void noneInspectitInput() {
            String property = "thisHasNothingToDoWithInspectit";
            assertThat(completer.checkPropertyName(property)).isEqualTo(false);
        }

    }

    @Nested
    public class ToCamelCase {
        @Test
        void kebabToCamel() {
            assertThat(completer.toCamelCase("i-want-to-be-camel-case")).isEqualTo("iWantToBeCamelCase");
        }
    }

    @Nested
    public class ToKebabCase {
        @Test
        void camelToKebab() {
            assertThat(completer.toKebabCase("iWantToBeKebabCase")).isEqualTo("i-want-to-be-kebab-case");
        }
    }

    @Nested
    public class CheckPropertyExists {
        @Test
        void checkFirstLevel() {
            ArrayList<String> input = new ArrayList<>(Arrays.asList("inspectit", "instrumentation"));
            ArrayList<String> output = new ArrayList<>(Arrays.asList("actions",
                    "class",
                    "data",
                    "exclude-lambdas",
                    "ignored-bootstrap-packages",
                    "ignored-packages",
                    "internal",
                    "rules",
                    "scopes",
                    "special"));
            assertThat(completer.findProperties(input, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void checkMap() {
            ArrayList<String> input = new ArrayList<>(Arrays.asList("inspectit", "metrics", "definitions"));
            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.findProperties(input, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void checkList() {
            ArrayList<String> input = new ArrayList<>(Arrays.asList("inspectit", "instrumentation", "scopes"));
            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.findProperties(input, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void pastMap() {
            ArrayList<String> input = new ArrayList<>(Arrays.asList("inspectit", "instrumentation", "scopes", "my-key"));
            ArrayList<String> output = new ArrayList<>(Arrays.asList("advanced",
                    "class",
                    "interfaces",
                    "methods",
                    "narrow-scope",
                    "superclass",
                    "type"));
            assertThat(completer.findProperties(input, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void pastList() {
            ArrayList<String> input = new ArrayList<>(Arrays.asList("matcher-mode"));
            ArrayList<String> output = new ArrayList<>(Arrays.asList("class",
                    "config",
                    "exporters",
                    "instrumentation",
                    "logging",
                    "metrics",
                    "self-monitoring",
                    "service-name",
                    "tags",
                    "thread-pool-size",
                    "tracing"));
            assertThat(completer.findPropertiesInList(input, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void endsInWildcard() {
            ArrayList<String> input = new ArrayList<>(Arrays.asList("inspectit", "instrumentation", "actions", "string_replace_all", "input", "regex"));
            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.findPropertiesInList(input, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void propertyIsPresentAndReadMethodIsNull() {
            ArrayList<String> input = new ArrayList<>(Arrays.asList("instrumentation", "data", "method_duration", "is-tag"));
            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.findProperties(input, InspectitConfig.class)).isEqualTo(output);
        }

    }

    @Nested
    public class FindValidPropertyNames {
        @Test
        void validProperty() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("class", "declaring-class"));
            assertThat(completer.findValidPropertyNames("inspectit.instrumentation.scopes.jdbc_statement_execute.interfaces[0].matcher-mode")).isEqualTo(output);

        }

        @Test
        void invalidProperty() {
            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.findValidPropertyNames("inspeit.instrumentation.scopes")).isEqualTo(output);

        }
    }

}
