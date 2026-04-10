package ru.daniil.user;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Tests сервисного слоя")
@SelectPackages("ru.daniil.user.service")
public class UserServiceTestsSuite {
}
