package cn.itech4u.service.im;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author by yuanlai
 * @Date 2020/7/8 10:35 上午
 * @Description: TODO
 * @Version 1.0
 */
@SpringBootApplication
public class Itech4uServiceIMApplication {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(Itech4uServiceIMApplication.class);
        springApplication.setBannerMode(Banner.Mode.CONSOLE);
        springApplication.run(args);
    }

}
