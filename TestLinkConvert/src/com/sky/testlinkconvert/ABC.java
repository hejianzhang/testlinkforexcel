package com.sky.testlinkconvert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2017-03-07.
 */
public class ABC {

    public static void main(String[] argc){

        String str = "<![CDATA[<ol>\n" +
                "    <li>&nbsp;12v熟练掌握Java主流框架 : Spring mvc 、Hibernate、Mybatis、分布式缓存（Memcache或Redis）、消息队列等</li>\n" +
                "    <li>练掌握Java主流框架 : Spring mvc 、Hibernate、Mybatis、分布式缓存（Memcache或Redis）、消息队列等</li>\n" +
                "    <li>33练掌握Java主流框架 : Spring mvc 、Hibernate、Mybatis、分布式缓存（Memcache或Redis）、消息队列等</li>\n" +
                "</ol>\n" +
                "<p>&nbsp;</p>]]>";

        Pattern pat = Pattern.compile("\\<li\\>(.*)\\</li\\>");

        Matcher ma =  pat.matcher(str);

        if(ma.find()){
            System.out.println(ma.group(1));
        }

    }

}
