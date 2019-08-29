package rocks.inspectit.ocelot.autocomplete;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.utils.CaseUtils;
import rocks.inspectit.ocelot.config.validation.Helper;
import rocks.inspectit.ocelot.config.validation.OrderEnum;

import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;

@Component
public class AutocompleterImpl {

    /**
     * A HashSet of classes which are used as wildcards in the search for properties. If a found class matches one of these
     * classes, the end of the property path is reached. Mainly used in the search of maps
     */
    private static final HashSet<Class<?>> TERMINAL_TYPES = new HashSet(Arrays.asList(Object.class, String.class, Integer.class, Long.class,
            Float.class, Double.class, Character.class, Void.class,
            Boolean.class, Byte.class, Short.class, Duration.class));

    @Autowired
    private Helper help = new Helper();

    @Autowired
    private CaseUtils utils;

    /**
     * Method to find properties in an existing path
     * Used to give suggestions for the completion for the path
     *
     * @param propertyName A String containing the path
     * @return A List of Strings which can be used in the given path
     */
    public List<String> findValidPropertyNames(String propertyName) {
        if (checkPropertyName(propertyName)) {
            return collectProperties((ArrayList<String>) help.parse(propertyName));
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
        ArrayList<String> list = ((ArrayList<String>) help.parse(propertyName));
        return propertyName != null
                && propertyName.startsWith("inspectit.")
                && help.checkPropertyExists(list.subList(1, list.size()), InspectitConfig.class) != OrderEnum.EXISTS_NOT;
    }


    /**
     * Returns the names of the properties in a given path
     *
     * @param properties The path to a property one wants to recieve the properties of
     * @return The names of the properties of the given path as list
     */
    private List<String> collectProperties(ArrayList<String> properties) {
        properties.remove(0);
        Type type = InspectitConfig.class;
        Optional<PropertyDescriptor> foundProperty = null;
        while (properties.size() >= 1) {
            String propertyName = utils.kebabCaseToCamelCase(properties.get(0));
            type = getCurrentType(type, propertyName);
            if (type == null) {
                return new ArrayList<>(); //returned null before the end of the path was reached
            }
            properties.remove(0);
        }
        if (type instanceof ParameterizedType) {
            return new ArrayList<>(); //ended in a map
        }
        if (help.isTerminalOrEnum(type)) {
            return new ArrayList<>(); //ended in a terminal
        }
        return getProperties((Class<?>) type); //return current options

    }

    /**
     * Return the properties of a given class
     *
     * @param beanClass the class one wants the properties of
     * @return the properties of the given class
     */
    List<String> getProperties(Class<?> beanClass) {
        ArrayList<String> propertyList = new ArrayList<>();
        Arrays.stream(BeanUtils.getPropertyDescriptors(beanClass)).forEach(descriptor -> propertyList.add(utils.camelCaseToKebabCase(descriptor.getName())));
        return propertyList;

    }

    /**
     * returns the type of a map or a list
     *
     * @param type the ParameterizedType one wants the type of
     * @return the type of the list or map
     */
    Type getParamTypeArgs(ParameterizedType type) {
        int index = 1;
        if (type.getRawType() == List.class) {
            index = 0;
        }
        return type.getActualTypeArguments()[index];
    }

    Type getCurrentType(Type type, String propertyName) {
        if (type instanceof ParameterizedType) {
            return getParamTypeArgs((ParameterizedType) type);
        } else {
            Optional<PropertyDescriptor> foundProperty =
                    Arrays.stream(BeanUtils.getPropertyDescriptors((Class<?>) type))
                            .filter(descriptor -> descriptor.getName().equals(propertyName))
                            .findFirst();
            if (foundProperty.get().getReadMethod() != null) {
                return foundProperty.get().getReadMethod().getGenericReturnType();
            }

        }
        return null;
    }

}
