package rocks.inspectit.ocelot.autocomplete;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Component
public class Autocompleter {
    /**
     * A HashSet of classes which are used as wildcards in the search for properties. If a found class matches one of these
     * classes, the end of the property path is reached. Mainly used in the search of maps
     */
    private static final HashSet<Class<?>> WILDCARD_TYPES = new HashSet(Arrays.asList(Object.class, String.class, Integer.class, Long.class,
            Float.class, Double.class, Character.class, Void.class,
            Boolean.class, Byte.class, Short.class));

    /**
     * Method to find properties in an existing path
     * Used to give suggestions for the completion for the path
     *
     * @param propertyName A String containing the path
     * @return A List of Strings which can be used in the given path
     */
    public List<String> findValidPropertyNames(String propertyName) {
        if (checkPropertyName(propertyName)) {
            return findProperties(parse(propertyName), InspectitConfig.class);
        }
        return new ArrayList<>();
    }

    /**
     * Checks if a given path could exist
     *
     * @param propertyName
     * @return True: the given path could exist <br> False: the given path does not exist
     */
    boolean checkPropertyName(String propertyName) {
        return propertyName != null
                && propertyName.length() > 8
                && propertyName.startsWith("inspectit");
    }

    /**
     * Helper method for findProperties
     * This method takes an array of strings and returns each entry as ArrayList containing the parts of each element.
     * <p>
     * 'inspectit.hello-i-am-testing' would be returned as {'inspectit', 'helloIAmTesting'}
     *
     * @param propertyName A String Array containing property Strings
     * @return a ArrayList containing containing the parts of the property as String
     */
    List<String> parse(String propertyName) {
        ArrayList<String> result = new ArrayList<>();
        String remainder = propertyName;
        while (remainder != null && !remainder.isEmpty()) {
            remainder = extractExpression(remainder, result);
        }
        return result;
    }

    /**
     * Extracts the first path expression from the given propertyName and appends it to the given result list.
     * The remaidner of the proeprty name is returned
     * <p>
     * E.g. inspectit.test.rest -> "inspectit" is added to the list, "test.rest" is returned.
     * E.g. [inspectit.literal].test.rest -> "inspectit.literal" is added to the list, "test.rest" is returned.
     * E.g. [inspectit.literal][test].rest -> "inspectit.literal" is added to the list, "[test].rest" is returned.
     *
     * @param propertyName
     * @param result
     * @return
     */
    String extractExpression(String propertyName, List<String> result) {
        if (propertyName.startsWith("[")) {
            int end = propertyName.indexOf(']');
            if (end == -1) {
                throw new IllegalArgumentException("invalid property path");
            }
            result.add(propertyName.substring(1, end));
            return removeLeadingDot(propertyName.substring(end + 1));
        } else {
            int end = findFirstIndexOf(propertyName, '.', '[');
            if (end == -1) {
                result.add(propertyName);
                return "";
            } else {
                result.add(propertyName.substring(0, end));
                return removeLeadingDot(propertyName.substring(end));
            }
        }
    }

    private int findFirstIndexOf(String propertyName, char first, char second) {
        int firstIndex = propertyName.indexOf(first);
        int secondIndex = propertyName.indexOf(second);
        if (firstIndex == -1) {
            return secondIndex;
        } else if (secondIndex == -1) {
            return firstIndex;
        } else {
            return Math.min(firstIndex, secondIndex);
        }
    }

    private String removeLeadingDot(String string) {
        if (string.startsWith(".")) {
            return string.substring(1);
        } else {
            return string;
        }
    }

    /**
     * Helper-Method for parse
     * Takes any given String and converts it from kebab-case into camelCase
     * Strings without any dashes are returned unaltered
     *
     * @param name The String which should be changed into camelCase
     * @return the given String in camelCase
     */
    String toCamelCase(String name) {
        StringBuilder builder = new StringBuilder();
        String[] nameParts = name.split("-");
        boolean isFirst = true;
        for (String part : nameParts) {
            if (isFirst) {
                builder.append(part.toLowerCase());
                isFirst = false;
            } else if (!part.isEmpty()) {
                part = part.toLowerCase();
                part = part.substring(0, 1).toUpperCase() + part.substring(1);
                builder.append(part);
            }
        }
        return builder.toString();
    }

    /**
     * Parses camelCase into kebab-case
     *
     * @param str String to parsed
     * @return String parsed as kebab-case
     */
    String toKebabCase(String str) {
        StringBuilder builder = new StringBuilder();
        for (char c : str.toCharArray()
        ) {
            if (Character.isUpperCase(c)) {
                builder.append('-');
            }
            builder.append(Character.toLowerCase(c));
        }
        return builder.toString();
    }

    /**
     * Checks finds properties in a given path
     *
     * @param propertyNames The given path as list
     * @param type          The type in which the current top-level properties should be found
     * @return A list of properties which could be added to the current path
     */
    List<String> findProperties(List<String> propertyNames, Type type) {
        propertyNames.remove("inspectit");
        if (propertyNames.isEmpty()) {
            if (type instanceof Class) {
                return getProperties((Class<?>) type);
            } else {
                return new ArrayList<>();
            }
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType genericType = (ParameterizedType) type;
            if (genericType.getRawType() == Map.class) {
                return findPropertiesInMap(propertyNames, genericType.getActualTypeArguments()[1]);
            } else if (genericType.getRawType() == List.class) {
                return findPropertiesInList(propertyNames, genericType.getActualTypeArguments()[0]);
            }
        }
        if (type instanceof Class) {
            return findPropertiesInBean(propertyNames, (Class<?>) type);
        } else {
            throw new IllegalArgumentException("Unexpected type: " + type);
        }

    }

    /**
     * Checks if a given path ends in a map or moves on beyond it.
     *
     * @param propertyNames List of property names
     * @param mapValueType  The type which is given as value type of a map
     * @return Returns an empty Array if the given path ends in a map <br> Otherwise keeps going until the end of the
     * given path is reached and returns the corresponding value
     */
    private List<String> findPropertiesInMap(List<String> propertyNames, Type mapValueType) {
        if (WILDCARD_TYPES.contains(mapValueType)) {
            return new ArrayList<>();
        } else {
            return findProperties(propertyNames.subList(1, propertyNames.size()), mapValueType);
        }
    }

    /**
     * Checks if a given path ends in a map or moves on beyond it.
     *
     * @param propertyNames List of property names
     * @param listValueType The type which is given as value type of a list
     * @return Returns an empty Array if the given path ends in a list <br> Otherwise keeps going until the end of the
     * * given path is reached and returns the corresponding value
     */
    List<String> findPropertiesInList(List<String> propertyNames, Type listValueType) {
        return findProperties(propertyNames.subList(1, propertyNames.size()), listValueType);
    }

    /**
     * Checks a given path ends in a bean or moves beyond it
     *
     * @param propertyNames List of property names
     * @param beanType      The bean through which should be searched
     * @return Returns the beans properties if the given path ends in the bean <br> Otherwise keeps going until the
     * end of the given path is reached and returns the corresponding value
     */
    private List<String> findPropertiesInBean(List<String> propertyNames, Class<?> beanType) {
        String propertyName = toCamelCase(propertyNames.get(0));
        Optional<PropertyDescriptor> foundProperty =
                Arrays.stream(BeanUtils.getPropertyDescriptors(beanType))
                        .filter(descriptor -> descriptor.getName().equals(propertyName))
                        .findFirst();
        if (foundProperty.isPresent()) {
            if (foundProperty.get().getReadMethod() == null) {
                ArrayList<String> fieldNames = new ArrayList<>();
                if (!WILDCARD_TYPES.contains(beanType)) {
                    for (Field field : beanType.getFields()) {
                        fieldNames.add(field.getName());
                    }
                }
                return fieldNames;
            }
            Type propertyType = foundProperty.get().getReadMethod().getGenericReturnType();
            return findProperties(propertyNames.subList(1, propertyNames.size()), propertyType);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Returns the property names of a given class as list
     *
     * @param beanClass The class one wants to recieve the properties of
     * @return The property names as list
     */
    List<String> getProperties(Class<?> beanClass) {
        ArrayList<String> propertyList = new ArrayList<>();
        Arrays.stream(BeanUtils.getPropertyDescriptors(beanClass)).forEach(descriptor -> propertyList.add(toKebabCase(descriptor.getName())));
        return propertyList;

    }
}
