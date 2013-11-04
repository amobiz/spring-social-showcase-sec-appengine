Sample of using Spring Social on Google App Engine
==================================================

#### Forks from:

[spring-projects / spring-social-samples](https://github.com/spring-projects/spring-social-samples)

[vtserman / spring-social-appengine](https://github.com/vtserman/spring-social-appengine)

#### This sample applies dirty hacks on these issues:

[ProviderSignInAttempt - NotSerializableException: ConnectionFactoryRegistry](https://jira.springsource.org/browse/SOCIAL-203)

[Persistent session managment - NotSerializableException for OAuth2Connection and OAuth1Connection](https://jira.springsource.org/browse/SOCIAL-355)

[SocialAuthenticationProvider should support multiple connections](https://jira.springsource.org/browse/SOCIAL-402)

#### Notes on my dirty hack:

Changes the references to non-serializable objects transient:

* org.springframework.social.connect.web.ProviderSignInAttempt
* org.springframework.social.security.SocialAuthenticationToken

Fix the if condition directly:

* org.springframework.social.security.SocialAuthenticationProvider

#### Google App Engine & Maven Configuration

* Add library and plugin dependencies

  According to this guide: [Using Apache Maven](https://developers.google.com/appengine/docs/java/tools/maven), add Google App Engine maven library and plugin dependencies to pom.xml.

* Enable session

  [Java Application Configuration with appengine-web.xml - Enabling sessions](https://developers.google.com/appengine/docs/java/config/appconfig#Enabling_Sessions)

Spring Social Showcase Security
===============================
This sample app demonstrates many of the capabilities of the Spring Social project, including:
* Connect to Facebook
* Connect to Twitter
* Sign in using Facebook
* Sign in using Twitter
* Using SocialAuthenticationFilter for provider-signin instead of ProviderSignInController

To run, simply import the project into your IDE and deploy to a Servlet 2.5 or > container such as Tomcat 6 or 7.
Access the project at http://localhost:8080/spring-social-showcase

Discuss at forum.springsource.org and collaborate with the development team at jira.springframework.org/browse/SOCIAL.

