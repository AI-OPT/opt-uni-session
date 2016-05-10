package com.ai.opt.uni.session.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ai.opt.sdk.components.mcs.MCSClientFactory;
import com.ai.paas.ipaas.mcs.interfaces.ICacheClient;

/**
 * redis的客户端实现
 */
public class SessionClient {
	  private static final Log LOG = LogFactory.getLog(SessionClient.class);

    //private static final String SESSION_PAAS_NAMESPACE = "com.ai.runner.uni.session.SessionClient";
    private static String SESSION_PAAS_NAMESPACE  ;
    private static final String PROPERTIES_FILE_NAME = "unisession";
    private static final String PROPERTIES_KEY = "SESSION_PAAS_NAMESPACE";
    
    public static String getSessionPassNameSpace() {
    	LOG.debug("【-_-】SessionClient.getSessionPassNameSpace() begin...");
    	if(SESSION_PAAS_NAMESPACE == null) {
    		try {
    			ResourceBundle bundle = ResourceBundle.getBundle(PROPERTIES_FILE_NAME);
    			SESSION_PAAS_NAMESPACE = bundle.getString(PROPERTIES_KEY).trim();
    			LOG.debug("【-_-】统一缓存命名空间为配置值:"+SESSION_PAAS_NAMESPACE+"【-_-】");
    		}
    		catch(Exception ex) {
    			LOG.error( "Can't Load unisession.properties...",ex);
    		}
    	}
    	LOG.debug("【-_-】SessionClient.getSessionPassNameSpace() 统一缓存命名空间为配置值："+SESSION_PAAS_NAMESPACE);
    	return SESSION_PAAS_NAMESPACE;
    }

    public static ICacheClient getCacheClient() {
    	long t1=System.currentTimeMillis();   
    	LOG.debug("【-_-】SessionClient.getCacheClient() begin.........................");
    	String NameSpace = getSessionPassNameSpace();
    	LOG.debug("【-_-】SessionClient.getCacheClient() 统一缓存命名空间为配置值："+NameSpace);
    	ICacheClient CacheClient =  MCSClientFactory.getCacheClient(NameSpace);
    	long t2=System.currentTimeMillis()-t1;   

    	LOG.debug("【-_-】SessionClient.getCacheClient() end........" +
    			"{ CacheClientBuilderFactory.getCacheClientBuilder().getCacheClientcost(NameSpace) cost time: " +t2);
    	return CacheClient;
    }
    public void addItem(String key, Object object, int seconds) {
    	long t1=System.currentTimeMillis();   
    	LOG.debug("SessionClient。addItem ：key="+key+"begin");
    	getCacheClient().setex(key.getBytes(), seconds, serialize(object));
    	long t2=System.currentTimeMillis()-t1;   
    	LOG.debug("SessionClient。addItem ：key="+key+"end .{ getCacheClient().setex cost time: " +t2);
    }

    private static byte[] serialize(Object object) {
        if (object == null)
            return null;
        ByteArrayOutputStream baos = null;
        ObjectOutputStream objectOutput = null;
        try {
            baos = new ByteArrayOutputStream();
            objectOutput = new ObjectOutputStream(baos);
            objectOutput.writeObject(object);
            byte[] bytes = baos.toByteArray();
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != baos)
                    baos.close() ;
                if (null != objectOutput) {
                    objectOutput = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public long delItem(String key) {
    	LOG.debug("SessionClient。delItem ：key="+key+"begin");
    	return	getCacheClient().del(key.getBytes());
    }

    /**
     * 统一session使用
     *
     * @param key
     * @return
     */
    public Object getSession(String key) {
        byte[] data = null;
        long t1=System.currentTimeMillis();   
    	LOG.debug("SessionClient。getSession ：key="+key+"begin");
        data = getCacheClient().get(key.getBytes());
        long t2=System.currentTimeMillis()-t1;   
        LOG.debug("SessionClient。getSession ：key="+key+"end .{ getCacheClient().hget cost time: " +t2);
        return deserialize(data);
    }

    private static Object deserialize(byte[] bytes) {
        if (bytes == null)
            return null;
        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;
        try {
            bais = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != bais)
                    bais.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
