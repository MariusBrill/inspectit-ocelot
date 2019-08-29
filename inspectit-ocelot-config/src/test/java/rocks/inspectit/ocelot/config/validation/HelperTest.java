package rocks.inspectit.ocelot.config.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HelperTest {

    Helper helper;

    @BeforeEach
    void buildHelper() {
        helper = new Helper();
    }

    @Nested
    public class Parse {
        @Test
        void kebabCaseTest() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "iCan-parse-kebab", "case", "even-in-brackets\\wow", "thisIs-awesome"));

            assertThat(helper.parse("inspectit.iCan-parse-kebab.case[even-in-brackets\\wow].thisIs-awesome")).isEqualTo(output);
        }

        @Test
        void emptyString() {
            ArrayList<String> output = new ArrayList<>();

            assertThat(helper.parse("")).isEqualTo(output);
        }

        @Test
        void nullString() {
            ArrayList<String> output = new ArrayList<>();

            assertThat(helper.parse(null)).isEqualTo(output);

        }

        @Test
        void bracketAfterBracket() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "property", "first", "second"));

            assertThat(helper.parse("inspectit.property[first][second]")).isEqualTo(output);
        }

        @Test
        void dotInBrackets() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "property", "first.second"));

            assertThat(helper.parse("inspectit.property[first.second]")).isEqualTo(output);
        }

        @Test
        void throwsException() {
            try {
                helper.parse("inspectit.property[first.second");
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isEqualTo("invalid property path");
            }
        }

    }

    @Nested
    public class CheckPropertyExists {
        @Test
        void termminalTest() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("config", "file-based", "path"));

            assertThat(helper.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(OrderEnum.EXISTS_PATH_END);
        }

        @Test
        void nonTermminalTest() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("exporters", "metrics", "prometheus"));

            assertThat(helper.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(OrderEnum.EXISTS_NON_PATH_END);
        }

        @Test
        void emptyString() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList(""));

            assertThat(helper.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(OrderEnum.EXISTS_NOT);
        }

        @Test
        void existingList() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("instrumentation", "scopes", "jdbc_statement_execute", "interfaces", "0", "matcher-mode"));

            assertThat(helper.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(OrderEnum.EXISTS_PATH_END);
        }

        @Test
        void existingMap() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("metrics", "definitions", "jvm/gc/concurrent/phase/time", "description"));

            assertThat(helper.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(OrderEnum.EXISTS_PATH_END);
        }

        @Test
        void readMethodIsNull() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("instrumentation", "data", "method_duration", "is-tag"));

            assertThat(helper.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(OrderEnum.EXISTS_PATH_END);
        }

        @Test
        void endsInWildcardType() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("instrumentation", "actions", "string_replace_all", "input", "regex"));

            assertThat(helper.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(OrderEnum.EXISTS_PATH_END);
        }
    }

    @Nested
    public class CheckPropertyExistsInMap {
        @Test
        void nonTerminalMapTest() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("matcher-mode"));

            assertThat(helper.checkPropertyExistsInMap(list, Map.class)).isEqualTo(OrderEnum.EXISTS_NON_PATH_END);

        }
    }

    @Nested
    public class CheckPropertyExistsInList {
        @Test
        void nonTerminalListTest() {
            ArrayList<String> list = new ArrayList(Arrays.asList("instrumentation", "scopes", "jdbc_statement_execute", "interfaces", "0", "matcher-mode"));

            assertThat(helper.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(OrderEnum.EXISTS_PATH_END);
        }
    }
}
