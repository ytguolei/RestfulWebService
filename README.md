# RestfulWebService

Spring FrameWork Restful WebService




1.	Restful  https服务调用:
1)、创建CloseableHttpClient  httpClient =HttpClientUtils.acceptsUntrustedCertsHttpClient();，引用jar包：
import org.apache.http.impl.client.CloseableHttpClient;
HttpClientUtils工具类： 

2）创建requestFactory:HttpComponentsClientHttpRequestFactory c =new HttpComponentsClientHttpRequestFactory(httpClient);
引用jar包：
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
3) 创建RestTemplate: RestTemplate restTemplate = new RestTemplate(c);
引用jar包：
import org.springframework.web.client.RestTemplate
4)添加验证：
创建用户信息String	base64ClientCredentials = new 
String(Base64.encodeBase64(“username:password”));
放入消息头中：
HttpHeaders requestHeaders = new HttpHeaders();
requestHeaders.add("Authorization", "Basic " + base64ClientCredentials);
5）将传输方式、编码格式及其他扩展信息放入消息头中，如：
requestHeaders.add("Accept", "application/json");
		requestHeaders.add("Content-Type", "application/json;charset=UTF-8");
		requestHeaders.add("X-SGM-MSG-ID", uuid);
		requestHeaders.add("X-SGM-FROM-SYS-ID", "ITSS");
		requestHeaders.add("X-SGM-TO-SYS-ID", "ITPM");
		requestHeaders.add("X-SGM-MSG-TS", sdf.format(date));

6）创建传递参数：
Map<String,Object> tomap = new HashMap<String,Object>();   
			tomap.put("projCode", projCode);  
			tomap.put("projScore", score);  
7）将信息放入HttpEntity中：
HttpEntity<Map> entity = new HttpEntity<Map>(toMap,requestHeaders);
8）发送信息获取返回值：
ResponseEntity<Map> response = restTemplate.postForEntity(
					addr, 
					entity, Map.class);
		Map  returnMap = response.getBody();
2.	Restful  http服务调用:
一: RestTemplate方式调用
1)	配置文件中增加
<bean id="httpClientFactory" 
class="org.springframework.http.client.SimpleClientHttpRequestFactory"> 
<property name="connectTimeout" value="10000" /> 
<property name="readTimeout" value="10000" /> 
</bean> 
<!--RestTemplate --> 
<bean id="restTemplate" class="org.springframework.web.client.RestTemplate"> 
	<constructor-arg ref="httpClientFactory" /> 
</bean>
	2）创建template ：
@Autowired 
private RestTemplate template;
3）将传输方式、编码格式及其他扩展信息放入消息头中，如：
		HttpHeaders requestHeaders = new HttpHeaders();
requestHeaders.add("Accept", "application/json");
		requestHeaders.add("Content-Type", "application/json;charset=UTF-8");
		requestHeaders.add("X-SGM-MSG-ID", uuid);
		requestHeaders.add("X-SGM-FROM-SYS-ID", "ITSS");
		requestHeaders.add("X-SGM-TO-SYS-ID", "ITPM");
		requestHeaders.add("X-SGM-MSG-TS", sdf.format(date));

4）创建传递参数：
Map<String,Object> tomap = new HashMap<String,Object>();   
			tomap.put("projCode", projCode);  
			tomap.put("projScore", score);  
5）将信息放入HttpEntity中：
HttpEntity<Map> entity = new HttpEntity<Map>(toMap,requestHeaders);
6）发送信息获取返回值：
ResponseEntity<Map> response = restTemplate.postForEntity(
					addr, 
					entity, Map.class);
		Map  returnMap = response.getBody();
	二: HttpClient调用
1）创建HttpClient对象：
HttpClient client = HttpClients.createDefault();
引用jar: import org.apache.http.client.HttpClient;
	 2）创建请求链接：
		HttpPost request = new HttpPost(addr);
引用jar: import org.apache.http.client.methods.HttpPost;
2)	添加标题信息：
request.setHeader("Accept", "application/json");
	  	request. setHeader ("Content-Type", "application/json;charset=UTF-8");
		request. setHeader ("X-SGM-MSG-ID", uuid);
		request. setHeader ("X-SGM-FROM-SYS-ID", "ITSS");
		request. setHeader ("X-SGM-TO-SYS-ID", "ITPM");
		request. setHeader ("X-SGM-MSG-TS", sdf.format(date));
3）配置传递参数：
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();  
         nvps.add(new BasicNameValuePair("projCode ", projCode));  
     nvps.add(new BasicNameValuePair("projScore ", score));
		UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nvps, "UTF-8");
request2.setEntity(formEntity);
		引用jar: import org.apache.http.client.entity.UrlEncodedFormEntity;
		import org.apache.http.NameValuePair;
4)	调用接口、获取返回信息
 			HttpResponse response2 = client.execute(request2);
            HttpEntity entity = response2.getEntity();
            ObjectMapper mapper = new ObjectMapper();
     Map  map = mapper.readValue(entity.getContent(),Map.class);
