package org.molgenis.lifelines;

import java.util.List;
import java.util.Properties;

import javax.xml.validation.Schema;

import nl.umcg.hl7.CatalogService;
import nl.umcg.hl7.GenericLayerCatalogService;

import org.molgenis.DatabaseConfig;
import org.molgenis.lifelines.catalogue.CatalogLoaderController;
import org.molgenis.lifelines.plugins.CatalogueLoaderPlugin;
import org.molgenis.lifelines.plugins.StudyDefinitionLoaderPlugin;
import org.molgenis.lifelines.resourcemanager.ResourceManagerService;
import org.molgenis.lifelines.studydefinition.StudyDefinitionLoaderController;
import org.molgenis.lifelines.utils.SchemaLoader;
import org.molgenis.lifelines.utils.SecurityHandlerInterceptor;
import org.molgenis.omx.OmxConfig;
import org.molgenis.util.ApplicationContextProvider;
import org.molgenis.util.AsyncJavaMailSender;
import org.molgenis.util.FileStore;
import org.molgenis.util.GsonHttpMessageConverter;
import org.molgenis.util.ShoppingCart;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

@Configuration
@EnableWebMvc
@EnableAsync
@ComponentScan("org.molgenis")
@Import(
{ DatabaseConfig.class, OmxConfig.class })
public class WebAppConfig extends WebMvcConfigurerAdapter
{
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry)
	{
		registry.addResourceHandler("/css/**").addResourceLocations("/css/", "classpath:/css/");
		registry.addResourceHandler("/img/**").addResourceLocations("/img/", "classpath:/img/");
		registry.addResourceHandler("/js/**").addResourceLocations("/js/", "classpath:/js/");
		registry.addResourceHandler("/generated-doc/**").addResourceLocations("/generated-doc/");
	}

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters)
	{
		converters.add(new GsonHttpMessageConverter());
		converters.add(new BufferedImageHttpMessageConverter());
	}

	@Bean
	public ApplicationListener<?> databasePopulator()
	{
		return new WebAppDatabasePopulator();
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer properties()
	{
		PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
		Resource[] resources = new FileSystemResource[]
		{ new FileSystemResource(System.getProperty("user.home") + "/molgenis-server.properties") };
		pspc.setLocations(resources);
		pspc.setFileEncoding("UTF-8");
		pspc.setIgnoreUnresolvablePlaceholders(true);
		pspc.setIgnoreResourceNotFound(true);
		return pspc;
	}

	@Value("${mail.host:smtp.gmail.com}")
	private String mailHost;
	@Value("${mail.port:587}")
	private Integer mailPort;
	@Value("${mail.protocol:smtp}")
	private String mailProtocol;
	@Value("${mail.username}")
	private String mailUsername; // specify in molgenis-server.properties
	@Value("${mail.password}")
	private String mailPassword; // specify in molgenis-server.properties
	@Value("${mail.java.auth:true}")
	private String mailJavaAuth;
	@Value("${mail.java.starttls.enable:true}")
	private String mailJavaStartTlsEnable;
	@Value("${mail.java.quitwait:false}")
	private String mailJavaQuitWait;

	@Bean
	public JavaMailSender mailSender()
	{
		AsyncJavaMailSender mailSender = new AsyncJavaMailSender();
		mailSender.setHost(mailHost);
		mailSender.setPort(Integer.valueOf(mailPort));
		mailSender.setProtocol(mailProtocol);
		mailSender.setUsername(mailUsername); // specify in
												// molgenis-server.properties
		mailSender.setPassword(mailPassword); // specify in
												// molgenis-server.properties
		Properties javaMailProperties = new Properties();
		javaMailProperties.setProperty("mail.smtp.auth", mailJavaAuth);
		javaMailProperties.setProperty("mail.smtp.starttls.enable", mailJavaStartTlsEnable);
		javaMailProperties.setProperty("mail.smtp.quitwait", mailJavaQuitWait);
		mailSender.setJavaMailProperties(javaMailProperties);
		return mailSender;
	}

	@Bean
	public FileStore fileStore()
	{
		return new FileStore(System.getProperty("user.home"));
	}

	/**
	 * Bean that allows referencing Spring managed beans from Java code which is not managed by Spring
	 * 
	 * @return
	 */
	@Bean
	public ApplicationContextProvider applicationContextProvider()
	{
		return new ApplicationContextProvider();

	}

	/**
	 * Enable spring freemarker viewresolver. All freemarker template names should end with '.ftl'
	 */
	@Bean
	public ViewResolver viewResolver()
	{
		FreeMarkerViewResolver resolver = new FreeMarkerViewResolver();
		resolver.setCache(true);
		resolver.setSuffix(".ftl");

		return resolver;
	}

	/**
	 * Configure freemarker. All freemarker templates should be on the classpath in a package called 'freemarker'
	 */
	@Bean
	public FreeMarkerConfigurer freeMarkerConfigurer()
	{
		FreeMarkerConfigurer result = new FreeMarkerConfigurer();
		result.setPreferFileSystemAccess(false);
		result.setTemplateLoaderPath("classpath:/templates/");

		return result;
	}

	@Bean
	public MultipartResolver multipartResolver()
	{
		return new StandardServletMultipartResolver();
	}

	@Bean
	@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, value = WebApplicationContext.SCOPE_SESSION)
	public ShoppingCart shoppingCart()
	{
		return new ShoppingCart();
	}

	@Bean
	public GenericLayerCatalogService genericLayerCatalogService()
	{
		return new CatalogService().getBasicHttpBindingGenericLayerCatalogService();
	}

	@Value("${lifelines.resource.manager.service.url}")
	private String resourceManagerServiceUrl;// Specify in molgenis-server.properties

	@Value("${lifelines.validate:false}")
	private boolean validate;// Specify in molgenis-server.properties, validate generic layer responses

	@Bean
	public ResourceManagerService resourceManagerService()
	{
		return new ResourceManagerService(resourceManagerServiceUrl, emeasureSchema(), validate);
	}

	@Bean
	public SecurityHandlerInterceptor catalogLoaderHandlerInterceptor()
	{
		return new SecurityHandlerInterceptor(CatalogueLoaderPlugin.class);
	}

	@Bean
	public MappedInterceptor catalogLoaderMappedInterceptor()
	{
		return new MappedInterceptor(new String[]
		{ CatalogLoaderController.BASE_URL + "/**" }, catalogLoaderHandlerInterceptor());
	}

	@Bean
	public MappedInterceptor studyDefinitionLoaderMappedInterceptor()
	{
		return new MappedInterceptor(new String[]
		{ StudyDefinitionLoaderController.BASE_URL + "/**" }, studyDefinitionLoaderHandlerInterceptor());
	}

	@Bean
	public SecurityHandlerInterceptor studyDefinitionLoaderHandlerInterceptor()
	{
		return new SecurityHandlerInterceptor(StudyDefinitionLoaderPlugin.class);
	}

	@Bean
	public Schema emeasureSchema()
	{
		return new SchemaLoader("EMeasure.xsd").getSchema();
	}

	/**
	 * Redirects '/' to the Home plugin
	 * 
	 */
	@Controller
	@RequestMapping("/")
	public static class RootController
	{
		@RequestMapping(method =
		{ RequestMethod.GET, RequestMethod.POST })
		public String index()
		{
			return "redirect:molgenis.do?__target=main&select=Home";
		}
	}
}