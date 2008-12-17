package org.springframework.security.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.security.providers.encoding.BaseDigestPasswordEncoder;
import org.springframework.security.providers.encoding.Md4PasswordEncoder;
import org.springframework.security.providers.encoding.Md5PasswordEncoder;
import org.springframework.security.providers.encoding.PasswordEncoder;
import org.springframework.security.providers.encoding.PlaintextPasswordEncoder;
import org.springframework.security.providers.encoding.ShaPasswordEncoder;
import org.springframework.security.providers.ldap.authenticator.LdapShaPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Stateful parser for the <password-encoder> element.
 *
 * Will produce a PasswordEncoder and (optionally) a SaltSource.
 *
 * @author Luke Taylor
 * @version $Id$
 */
class PasswordEncoderParser {
    static final String ATT_REF = "ref";
    static final String ATT_HASH = "hash";
    static final String ATT_BASE_64 = "base64";
    static final String OPT_HASH_PLAINTEXT = "plaintext";
    static final String OPT_HASH_SHA = "sha";
    static final String OPT_HASH_SHA256 = "sha-256";
    static final String OPT_HASH_MD4 = "md4";
    static final String OPT_HASH_MD5 = "md5";
    static final String OPT_HASH_LDAP_SHA = "{sha}";

    static final Map<String, Class<? extends PasswordEncoder>> ENCODER_CLASSES;

    static {
        ENCODER_CLASSES = new HashMap<String, Class<? extends PasswordEncoder>>(6);
        ENCODER_CLASSES.put(OPT_HASH_PLAINTEXT, PlaintextPasswordEncoder.class);
        ENCODER_CLASSES.put(OPT_HASH_SHA, ShaPasswordEncoder.class);
        ENCODER_CLASSES.put(OPT_HASH_SHA256, ShaPasswordEncoder.class);
        ENCODER_CLASSES.put(OPT_HASH_MD4, Md4PasswordEncoder.class);
        ENCODER_CLASSES.put(OPT_HASH_MD5, Md5PasswordEncoder.class);
        ENCODER_CLASSES.put(OPT_HASH_LDAP_SHA, LdapShaPasswordEncoder.class);
    }

    private Log logger = LogFactory.getLog(getClass());

    private BeanMetadataElement passwordEncoder;
    private BeanMetadataElement saltSource;

    public PasswordEncoderParser(Element element, ParserContext parserContext) {
        parse(element, parserContext);
    }

    private void parse(Element element, ParserContext parserContext) {
        String hash = element.getAttribute(ATT_HASH);
        boolean useBase64 = false;

        if (StringUtils.hasText(element.getAttribute(ATT_BASE_64))) {
            useBase64 = new Boolean(element.getAttribute(ATT_BASE_64)).booleanValue();
        }

        String ref = element.getAttribute(ATT_REF);

        if (StringUtils.hasText(ref)) {
            passwordEncoder = new RuntimeBeanReference(ref);
        } else {
            Class<? extends PasswordEncoder> beanClass = ENCODER_CLASSES.get(hash);
            RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass);

            if (OPT_HASH_SHA256.equals(hash)) {
                beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, new Integer(256));
            }

            beanDefinition.setSource(parserContext.extractSource(element));
            if (useBase64) {
                if (BaseDigestPasswordEncoder.class.isAssignableFrom(beanClass)) {
                    beanDefinition.getPropertyValues().addPropertyValue("encodeHashAsBase64", "true");
                } else {
                    logger.warn(ATT_BASE_64 + " isn't compatible with " + hash + " and will be ignored");
                }
            }
            passwordEncoder = beanDefinition;
        }

        Element saltSourceElt = DomUtils.getChildElementByTagName(element, Elements.SALT_SOURCE);

        if (saltSourceElt != null) {
            saltSource = new SaltSourceBeanDefinitionParser().parse(saltSourceElt, parserContext);
        }
    }

    public BeanMetadataElement getPasswordEncoder() {
        return passwordEncoder;
    }

    public BeanMetadataElement getSaltSource() {
        return saltSource;
    }
}
