spring-social-showcase-gae
==========================

Sample of using Spring Social on Google App Engine

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

