package com.companyname.at.mock;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.TextFile;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Optional;

public class FreemarkerTemplateTransformer extends ResponseDefinitionTransformer {

//  Name of Transformer - will be needed if you want refer to the transformation non-globally
  /*
  http://wiremock.org/docs/extending-wiremock/
  By default transformations will be applied globally.
  If you only want them to apply in certain cases you can refer to make them non-global by adding this to your transformer class:
    @Override
    public boolean applyGlobally() {
      return false;
    }


    Then you add the transformation to specific stubs via its name:
    stubFor(get(urlEqualTo("/local-transform")).willReturn(aResponse()
        .withStatus(200)
        .withBody("Original body")
        .withTransformers("my-transformer", "other-transformer")));
   */
  private static final String NAME = "freemarker-transformer";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean applyGlobally() {
//    return true;
    return false;
  }

  @Override
  public final ResponseDefinition transform(Request request,
      ResponseDefinition responseDefinition,
      FileSource files,
      Parameters parameters) {
    if (!responseDefinition.wasConfigured()) {
      return responseDefinition;
    }

    Optional<String> optionalTemplateBody = getTemplateBody(responseDefinition, files);
    if (!optionalTemplateBody.isPresent()) {
      return responseDefinition;
    }

    Template template = initTemplate(responseDefinition, optionalTemplateBody.get());
    String rendered = renderTemplate(template, parameters);

    ResponseDefinitionBuilder newResponseDefBuilder = ResponseDefinitionBuilder.like(responseDefinition);
    newResponseDefBuilder.withBody(rendered);
    return newResponseDefBuilder.build();
  }

  Template initTemplate(ResponseDefinition responseDefinition, String templateBody) {
    Configuration cfg = new Configuration(VERSION_2_3_23);
    cfg.setDefaultEncoding("UTF-8");
    Template template;
    String bodyFileName = responseDefinition.getBodyFileName();
    String templateName = isNotBlank(bodyFileName) ? bodyFileName : "<template>";
    try {
      template = new Template(templateName, new StringReader(templateBody), cfg);
    } catch (Exception e) {
      throw new RuntimeException(format("Failed to instantiate the template %s", templateName), e);
    }
    return template;
  }

  String renderTemplate(Template template, Parameters parameters) {
    Writer writer = new StringWriter();
    try {
      template.process(parameters, writer);
    } catch (Exception e) {
      throw new RuntimeException("Failed to render the template", e);
    }
    return writer.toString();
  }


  protected Optional<String> getTemplateBody(ResponseDefinition responseDefinition, FileSource files) {
    String templateBody = null;
    if (responseDefinition.specifiesBodyContent()) {
      templateBody = responseDefinition.getBody();
    } else if (responseDefinition.specifiesBodyFile()) {
      TextFile file = files.getTextFileNamed(responseDefinition.getBodyFileName());
      templateBody = file.readContentsAsString();
    }
    return Optional.ofNullable(templateBody);
  }

}
