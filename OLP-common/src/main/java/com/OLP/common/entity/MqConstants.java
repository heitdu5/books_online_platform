package com.OLP.common.entity;

public class MqConstants {

    /**
     * 交换机
     */
    public final static String BOOKS_EXCHANGE = "books.topic";

    /**
     * 监听新增和修改的队列
     */
    public final static String BOOKS_INSERTORUPDATE_QUEUE = "books.insert.queue";

    /**
     * 监听删除的队列
     */
    public final static String BOOKS_DELETE_QUEUE = "books.delete.queue";

    /**
     * 新增或修改的Routingkey
     */
    public final static String BOOKS_INSERTORUPDATE_KEY = "books.insert";

    /**
     * 删除的Routingkey
     */
    public final static String BOOKS_DELETE_KEY = "books.delete";


    /**
     * 点击量的队列
     */
    public final static String BOOKS_CLICK_QUEUE = "books.click.queue";




    /**
     * 点击量Routingkey
     */
    public final static String BOOKS_CLICK_KEY = "books.detail.click";



}
