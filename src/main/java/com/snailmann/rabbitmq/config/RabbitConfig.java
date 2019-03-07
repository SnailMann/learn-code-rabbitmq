package com.snailmann.rabbitmq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitConfig {


    private static final MessageConverter messageConverter = new Jackson2JsonMessageConverter();

    @Autowired
    private ConnectionFactory connectionFactory;


    @Bean
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * 消费者监听的普通连接工厂
     *
     * @return
     */
    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }


    /**
     * 消费者监听的手动ACK连接工厂
     *
     * @param connectionFactory
     * @return
     */
    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerManualAckContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        /**
         *  开启手动 ack
         */
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }


    /**
     * RabbitTempalte的普通工厂
     *
     * @return
     */
    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        rabbitTemplate.setConnectionFactory(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }


    /**
     * 支持生产者confirm模式的rabbitTemplate
     *
     * @return
     */
    @Bean
    public RabbitTemplate rabbitConfirmTemplate() {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        //1. 声明这是一个支持confirm机制的connectionFactory
        cachingConnectionFactory.setPublisherConfirms(true);
        cachingConnectionFactory.setHost(connectionFactory.getHost());
        cachingConnectionFactory.setPort(connectionFactory.getPort());
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        //2. 声明confirmCallback
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.warn("blocker success receive!! cause :{} and data: {}", cause, correlationData);
            } else {
                log.error("blocker failed receive!! cause :{} and data: {}", cause, correlationData);
            }
        });
        rabbitTemplate.setConnectionFactory(cachingConnectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }


    /**
     * 支持生产者return模式的rabbitTemplate
     *
     * @return
     */
    @Bean
    public RabbitTemplate rabbitReturnTemplate() {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        //1. 声明这是一个支持confirm机制的connectionFactory
        cachingConnectionFactory.setPublisherReturns(true);
        cachingConnectionFactory.setHost(connectionFactory.getHost());
        cachingConnectionFactory.setPort(connectionFactory.getPort());
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        //2. 声明强制性
        rabbitTemplate.setMandatory(true);
        //3. 声明returnCallback
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> log.warn("message: {} , replyCode: {} , replyText: {} , exchange: {} , routingKey:{}", message, replyCode, replyText, exchange, routingKey));
        rabbitTemplate.setConnectionFactory(cachingConnectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }


}
