# InstantiationAwareBeanPostProcessor

该接口是BeanPostProcessor的子接口，该接口也是回调接口，但是回调时机是在bean实例实例化前后，而不像BeanPostProcessor是在实例初始化前后。

该接口中方法的调用时机如下：

1. Bean实例实例化之前
2. Bean实例自动装配之前，或者是显式的属性配置之前

通常用于抑制特定目标Bean的默认实例化，例如创建具有特殊TargetSource的代理（池目标，延迟初始化目标等），或实现其他注入策略，例如字段注入。

该接口是专用接口，主要供框架内部使用。 建议尽可能实现普通的BeanPostProcessor接口，或从InstantiationAwareBeanPostProcessorAdapter派生，以免对该接口进行扩展。

## 主要方法

1. `postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException`方法

    ```java
        /**
        * Apply this BeanPostProcessor <i>before the target bean gets instantiated</i>.
        * 在目标bean被实例化之前应用该BeanPostProcessor。
        * The returned bean object may be a proxy to use instead of the target bean,
        * effectively suppressing default instantiation of the target bean.
        * 返回的bean对象可以是代替目标bean使用的代理，从而有效地抑制了目标bean的默认实例化。
        * <p>If a non-null object is returned by this method, the bean creation process
        * will be short-circuited.
        * 如果该方法返回的对象非空，那么bean的创建会被短路。
        * The only further processing applied is the
        * {@link #postProcessAfterInitialization} callback from the configured
        * {@link BeanPostProcessor BeanPostProcessors}.
        * 深入的使用方式是实现postProcessAfterInitialization回调方法
        * <p>This callback will be applied to bean definitions with their bean class,
        * as well as to factory-method definitions in which case the returned bean type
        * will be passed in here.
        * 此回调将应用于具有其bean类的bean定义以及工厂方法定义，在这种情况下，返回的bean类型将在此处传递。
        * <p>Post-processors may implement the extended
        * {@link SmartInstantiationAwareBeanPostProcessor} interface in order
        * to predict the type of the bean object that they are going to return here.
        * 后处理器可以实现扩展的SmartInstantiationAwareBeanPostProcessor接口，以便预测它们将在此处返回的Bean对象的类型。
        * <p>The default implementation returns {@code null}.
        * 默认情况下返回null
        * @param beanClass the class of the bean to be instantiated
        * @param beanName the name of the bean
        * @return the bean object to expose instead of a default instance of the target bean,
        * or {@code null} to proceed with default instantiation
        * @throws org.springframework.beans.BeansException in case of errors
        * @see #postProcessAfterInstantiation
        * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getBeanClass()
        * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getFactoryMethodName()
        */
        @Nullable
        default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
            return null;
        }
    ```

2. `postProcessAfterInstantiation(Object bean, String beanName) throws BeansException`方法

    ```java
        /**
        * Perform operations after the bean has been instantiated, via a constructor or factory method,
        * but before Spring property population (from explicit properties or autowiring) occurs.
        * 在实例通过构造器或者工厂方法实例化之后进行调用，但是在Spring属性注入之前（无论是显式属性设置或者自动注入）。
        * <p>This is the ideal callback for performing custom field injection on the given bean
        * instance, right before Spring's autowiring kicks in.
        * 这是在Spring的自动装配开始之前对给定的bean实例执行自定义字段注入的理想回调。
        * <p>The default implementation returns {@code true}.
        * 默认返回true
        * @param bean the bean instance created, with properties not having been set yet
        * @param beanName the name of the bean
        * @return {@code true} if properties should be set on the bean; {@code false}
        * if property population should be skipped. Normal implementations should return {@code true}.
        * Returning {@code false} will also prevent any subsequent InstantiationAwareBeanPostProcessor
        * instances being invoked on this bean instance.
        * @throws org.springframework.beans.BeansException in case of errors
        * @see #postProcessBeforeInstantiation
        */
        default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
            return true;
        }
    ```

3. `postProcessProperties(PropertyValues pvs, Object bean, String beanName)`方法

    ```java
        /**
        * Post-process the given property values before the factory applies them
        * to the given bean, without any need for property descriptors.
        * 在工厂将一些属性应用于bean之前，对给定的属性进行后处理，注意这些处理无需使用属性描述符。
        * <p>Implementations should return {@code null} (the default) if they provide a custom
        * {@link #postProcessPropertyValues} implementation, and {@code pvs} otherwise.
        * In a future version of this interface (with {@link #postProcessPropertyValues} removed),
        * the default implementation will return the given {@code pvs} as-is directly.
        * 如果实现提供自定义的postProcessPropertyValues实现，则实现应返回null
        * （默认值），否则返回pvs。 在此接口的将来版本中（删除了postProcessPropertyValues），
        * 默认实现将直接按原样返回给定的pvs。
        * @param pvs the property values that the factory is about to apply (never {@code null})
        * @param bean the bean instance created, but whose properties have not yet been set
        * @param beanName the name of the bean
        * @return the actual property values to apply to the given bean (can be the passed-in
        * PropertyValues instance), or {@code null} which proceeds with the existing properties
        * but specifically continues with a call to {@link #postProcessPropertyValues}
        * (requiring initialized {@code PropertyDescriptor}s for the current bean class)
        * @throws org.springframework.beans.BeansException in case of errors
        * @since 5.1
        * @see #postProcessPropertyValues
        */
        @Nullable
        default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
                throws BeansException {

            return null;
        }
    ```

4. `postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException`方法

    ```java
        /**
        * Post-process the given property values before the factory applies them
        * to the given bean.
        * Allows for checking whether all dependencies have been
        * satisfied, for example based on a "Required" annotation on bean property setters.
        * <p>Also allows for replacing the property values to apply, typically through
        * creating a new MutablePropertyValues instance based on the original PropertyValues,
        * adding or removing specific values.
        * <p>The default implementation returns the given {@code pvs} as-is.
        * @param pvs the property values that the factory is about to apply (never {@code null})
        * @param pds the relevant property descriptors for the target bean (with ignored
        * dependency types - which the factory handles specifically - already filtered out)
        * @param bean the bean instance created, but whose properties have not yet been set
        * @param beanName the name of the bean
        * @return the actual property values to apply to the given bean (can be the passed-in
        * PropertyValues instance), or {@code null} to skip property population
        * @throws org.springframework.beans.BeansException in case of errors
        * @see #postProcessProperties
        * @see org.springframework.beans.MutablePropertyValues
        * @deprecated as of 5.1, in favor of {@link #postProcessProperties(PropertyValues, Object, String)}
        */
        @Deprecated
        @Nullable
        default PropertyValues postProcessPropertyValues(
                PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

            return pvs;
        }
    ```

    该方法已被弃用，因此此处不再讲解。

## Spring针对于该接口的处理

这里结合Spring的Bean初始化逻辑来指出Spring如何使用上述接口。

1. Spring创建Bean的短路处理

    考察AbstractAutowireCapableBeanFactory类的resolveBeforeInstantiation方法：

    ```java
        protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
            Object bean = null;
            if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
                // Make sure bean class is actually resolved at this point.
                if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
                    Class<?> targetType = determineTargetType(beanName, mbd);
                    if (targetType != null) {
                        bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
                        if (bean != null) {
                            bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
                        }
                    }
                }
                mbd.beforeInstantiationResolved = (bean != null);
            }
            return bean;
        }
    ```

    Spring的Bean实例在创建之前先使用`applyBeanPostProcessorsBeforeInstantiation`方法获取bean，而该方法内部则调用了`postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException`方法，当返回的bean不为null时，则直接返回bean，不再创建bean。

2. Bean属性注入

    考察`AbstractAutowireCapableBeanFactory`的`populateBean`方法：

    ```java
    protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
        if (bw == null) {
            if (mbd.hasPropertyValues()) {
                throw new BeanCreationException(
                        mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
            }
            else {
                // Skip property population phase for null instance.
                return;
            }
        }

        // Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
        // state of the bean before properties are set. This can be used, for example,
        // to support styles of field injection.
        boolean continueWithPropertyPopulation = true;

        if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof InstantiationAwareBeanPostProcessor) {
                    InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                    if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                        continueWithPropertyPopulation = false;
                        break;
                    }
                }
            }
        }

        if (!continueWithPropertyPopulation) {
            return;
        }
        ...
    }
    ```

    该方法用于填充对象的属性，属性填充时会先寻找所有的InstantiationAwareBeanPostProcessor，调用其`postProcessAfterInstantiation`方法，如果返回false，则不再进行属性填充。
