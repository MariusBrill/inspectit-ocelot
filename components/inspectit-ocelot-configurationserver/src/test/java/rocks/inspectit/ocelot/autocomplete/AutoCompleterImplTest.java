package rocks.inspectit.ocelot.autocomplete;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;


public class AutoCompleterImplTest {

    @Autowired
    AutocompleterImpl completer;

    @BeforeEach
    void buildCompleter() {
        completer = new AutocompleterImpl();
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
    public class CheckPropertyExists {
        @Test
        void checkFirstLevel() {
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
            assertThat(completer.findValidPropertyNames("inspectit.instrumentation")).isEqualTo(output);
        }

        @Test
        void checkMap() {
            ArrayList<String> output = new ArrayList<>();

            assertThat(completer.findValidPropertyNames("inspectit.metrics.definitions")).isEqualTo(output);
        }

        @Test
        void checkList() {
            ArrayList<String> output = new ArrayList<>();

            assertThat(completer.findValidPropertyNames("inspectit.instrumentation.scopes")).isEqualTo(output);
        }

        @Test
        void pastMap() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("advanced",
                    "class",
                    "interfaces",
                    "methods",
                    "narrow-scope",
                    "superclass",
                    "type"));
            assertThat(completer.findValidPropertyNames("inspectit.instrumentation.scopes.my-key")).isEqualTo(output);
        }

        @Test
        void pastList() {
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
            assertThat(completer.findValidPropertyNames("inspectit.")).isEqualTo(output);
        }

        @Test
        void endsInWildcard() {
            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.findValidPropertyNames("inspectit.instrumentation.actions.string_replace_all.input.regex")).isEqualTo(output);
        }

        @Test
        void propertyIsPresentAndReadMethodIsNull() {
            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.findValidPropertyNames("inspectit.instrumentation.data.method_duration.is-tag")).isEqualTo(output);
        }

        @Test
        void hHopeThisWorks() {

            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.findValidPropertyNames("inspectit.instrumentation.scopes.jdbc_statement_execute.interfaces.[0].matcher-mode")).isEqualTo(output);
        }

        @Test
        void makeItRain() {
            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.findValidPropertyNames("inspectit.instrumentation.data.method_duration.is-tag")).isEqualTo(output);
        }


    }

    @Nested
    public class FindValidPropertyNames {
        @Test
        void validProperty() {
            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.findValidPropertyNames("inspectit.instrumentation.scopes.jdbc_statement_execute.interfaces[0].matcher-mode")).isEqualTo(output);

        }

        @Test
        void invalidProperty() {
            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.findValidPropertyNames("inspeit.instrumentation.scopes")).isEqualTo(output);

        }
    }

}
