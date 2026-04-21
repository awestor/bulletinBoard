package ru.daniil.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.daniil.cache.CacheModuleConfig;
import ru.daniil.comment.config.CommentModuleConfig;
import ru.daniil.discount.config.DiscountModuleConfig;
import ru.daniil.image.config.ImageModuleConfig;
import ru.daniil.order.config.OrderModuleConfig;
import ru.daniil.product.config.ProductModuleConfig;
import ru.daniil.testData.config.TestDataModuleConfig;
import ru.daniil.user.config.UserModuleConfig;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@Import({
		OrderModuleConfig.class,
		ProductModuleConfig.class,
		UserModuleConfig.class,
		ImageModuleConfig.class,
		CacheModuleConfig.class,
		TestDataModuleConfig.class,
		CommentModuleConfig.class,
		DiscountModuleConfig.class
})
public class BulletinBoardApplication {

	public static void main(String[] args) {
		SpringApplication.run(BulletinBoardApplication.class, args);
	}

}
