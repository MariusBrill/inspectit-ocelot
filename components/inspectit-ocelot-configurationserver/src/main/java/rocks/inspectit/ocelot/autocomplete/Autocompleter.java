package rocks.inspectit.ocelot.autocomplete;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.StringParser;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Component
public class Autocompleter {

    @Autowired
    StringParser stringParser;

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
            return findProperties(stringParser.parse(propertyName), InspectitConfig.class);
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
        if (WILDCARD_TYPES.contains(listValueType)) {
            return new ArrayList<>();
        } else {
            return findProperties(propertyNames.subList(1, propertyNames.size()), listValueType);
        }
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
        String propertyName = stringParser.toCamelCase(propertyNames.get(0));
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
        Arrays.stream(BeanUtils.getPropertyDescriptors(beanClass)).forEach(descriptor -> propertyList.add(stringParser.toKebabCase(descriptor.getName())));
        return propertyList;

    }
}
