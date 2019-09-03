package rocks.inspectit.ocelot.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import javax.annotation.PostConstruct;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Slf4j
@Component
public class YamlValidator {

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private StringParser stringParser;

    /**
     * A HashSet of classes which are used as wildcards in the search for properties. If a found class matches one of these
     * classes, the end of the property path is reached. Mainly used in the search of maps
     */
    private static final HashSet<Class<?>> WILDCARD_TYPES = new HashSet(Arrays.asList(Object.class, String.class, Integer.class, Long.class,
            Float.class, Double.class, Character.class, Void.class,
            Boolean.class, Byte.class, Short.class));

    @PostConstruct
    public void startStringFinder() {
        env.readPropertySources(propertySources -> {
            propertySources.stream()
                    .filter(ps -> ps instanceof EnumerablePropertySource)
                    .map(ps -> (EnumerablePropertySource) ps)
                    .flatMap(ps -> Arrays.stream(ps.getPropertyNames()))
                    .filter(ps -> checkPropertyName(ps))
                    .forEach(ps -> log.warn("Expression could not be resolved to a property: " + ps));
        });
    }

    /**
     * Checks if a propertyName should be added to the List of unmappedStrings or not
     *
     * @param propertyName
     * @return True: the propertyName does not exists as path <br> False: the propertyName exists as path
     */
    boolean checkPropertyName(String propertyName) {
        return propertyName != null
                && propertyName.startsWith("inspectit.")
                && !checkPropertyExists(parse(propertyName), InspectitConfig.class);
    }

    /**
     * Checks if a given List of properties exists as path
     *
     * @param propertyNames The list of properties one wants to check
     * @param type          The type in which the current top-level properties should be found
     * @return True: when the property exsits <br> False: when it doesn't
     */
    boolean checkPropertyExists(List<String> propertyNames, Type type) {
        propertyNames.remove("inspectit");
        if (propertyNames.isEmpty()) {
            return true; //base case
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType genericType = (ParameterizedType) type;
            if (genericType.getRawType() == Map.class) {
                return checkPropertyExistsInMap(propertyNames, genericType.getActualTypeArguments()[1]);
            } else if (genericType.getRawType() == List.class) {
                return checkPropertyExistsInList(propertyNames, genericType.getActualTypeArguments()[0]);
            }
        }
        if (type instanceof Class) {
            return checkPropertyExistsInBean(propertyNames, (Class<?>) type);
        } else {
            throw new IllegalArgumentException("Unexpected type: " + type);
        }
    }

    /**
     * Checks if a given type exists as value type in a map, keeps crawling through the given propertyName list
     *
     * @param propertyNames List of property names
     * @param mapValueType  The type which is given as value type of a map
     * @return True: The type exists <br> False: the type does not exists
     */
    boolean checkPropertyExistsInMap(List<String> propertyNames, Type mapValueType) {
        if (WILDCARD_TYPES.contains(mapValueType)) {
            return true;
        } else {
            return checkPropertyExists(propertyNames.subList(1, propertyNames.size()), mapValueType);
        }
    }

    /**
     * Checks if a given type exists as value type in a list, keeps crawling through the given propertyName list
     *
     * @param propertyNames List of property names
     * @param listValueType The type which is given as value type of a list
     * @return True: The type exists <br> False: the type does not exists
     */
    boolean checkPropertyExistsInList(List<String> propertyNames, Type listValueType) {
        return checkPropertyExists(propertyNames.subList(1, propertyNames.size()), listValueType);
    }

    /**
     * Checks if the first entry of the propertyNames list exists as property in a given bean
     *
     * @param propertyNames List of property names
     * @param beanType      The bean through which should be searched
     * @return True: the property and all other properties exists <br> False: At least one of the properties does not exist
     */
    boolean checkPropertyExistsInBean(List<String> propertyNames, Class<?> beanType) {
        String propertyName = stringParser.toCamelCase(propertyNames.get(0));
        Optional<PropertyDescriptor> foundProperty =
                Arrays.stream(BeanUtils.getPropertyDescriptors(beanType))
                        .filter(descriptor -> descriptor.getName().equals(propertyName))
                        .findFirst();
        if (foundProperty.isPresent()) {
            if (foundProperty.get().getReadMethod() == null) {
                return true;
            }
            Type propertyType = foundProperty.get().getReadMethod().getGenericReturnType();
            return checkPropertyExists(propertyNames.subList(1, propertyNames.size()), propertyType);
        } else {
            return false;
        }
    }
}
