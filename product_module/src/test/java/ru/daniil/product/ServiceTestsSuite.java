package ru.daniil.product;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Tests сервисного слоя")
@SelectPackages("ru.daniil.product.service")
public class ServiceTestsSuite {
}
