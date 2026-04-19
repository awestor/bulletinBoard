package ru.daniil.order;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Tests сервисного слоя заказов")
@SelectPackages("ru.daniil.order.orderService")
public class ServiceTestsSuite {
}
