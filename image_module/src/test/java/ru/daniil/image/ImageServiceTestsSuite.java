package ru.daniil.image;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Tests сервисного слоя")
@SelectPackages("ru.daniil.image.service")
public class ImageServiceTestsSuite {
}
