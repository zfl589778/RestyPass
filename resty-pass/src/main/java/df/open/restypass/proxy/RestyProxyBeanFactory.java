package df.open.restypass.proxy;

import df.open.restypass.base.DefaultRestyPassFactory;
import df.open.restypass.command.RestyCommandContext;
import df.open.restypass.executor.CommandExecutor;
import df.open.restypass.executor.FallbackExecutor;
import df.open.restypass.lb.server.ServerContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 说明:
 * <p/>
 * Copyright: Copyright (c)
 * <p/>
 * Company:
 * <p/>
 *
 * @author darren-fu
 * @version 1.0.0
 * @contact 13914793391
 * @date 2016/11/22
 */
@Data
@Slf4j
public class RestyProxyBeanFactory implements FactoryBean<Object>, InitializingBean, ApplicationContextAware {

    private Class<?> type;

    private RestyCommandContext restyCommandContext;

    private ApplicationContext applicationContext;

    private ServerContext serverContext;

    private CommandExecutor commandExecutor;

    private FallbackExecutor fallbackExecutor;


    private AtomicBoolean inited = new AtomicBoolean(false);

    @Override
    public Object getObject() throws Exception {
        return createProxy(type, restyCommandContext);
    }

    @Override
    public Class<?> getObjectType() {
        return type;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    protected Object createProxy(Class type, RestyCommandContext restyCommandContext) {

        if (!inited.get() && inited.compareAndSet(false, true)) {
            this.serverContext = getBean(ServerContext.class);
            this.commandExecutor = getBean(CommandExecutor.class);
            this.fallbackExecutor = getBean(FallbackExecutor.class);
        }
        Object proxy = null;
        try {
            RestyProxyInvokeHandler interfaceIvkHandler =
                    new RestyProxyInvokeHandler(restyCommandContext, commandExecutor, fallbackExecutor, serverContext);
            proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, interfaceIvkHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return proxy;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(type, "type不能为空");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    private <T> T getBean(Class<T> clz) {
        T t = null;
        try {
            if (this.applicationContext != null) {
                t = this.applicationContext.getBean(clz);
            }
            if (t == null) {
                log.info("{}使用默认配置", clz);
                t = DefaultRestyPassFactory.getDefaultBean(clz);
            } else {
                log.info("{}使用Spring注入", clz);

            }
        } catch (Exception ex) {
            log.info("{}使用默认配置,ex:{}", clz, ex.getMessage());
            t = DefaultRestyPassFactory.getDefaultBean(clz);
        }
        return t;
    }
}