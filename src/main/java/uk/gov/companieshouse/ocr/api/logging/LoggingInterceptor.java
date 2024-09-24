package uk.gov.companieshouse.ocr.api.logging;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.logging.util.RequestLogger;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;

@Component
public class LoggingInterceptor implements HandlerInterceptor, RequestLogger {
	private static final Logger LOGGER = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		logStartRequestProcessing(request, LOGGER);
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) {
		logEndRequestProcessing(request, response, LOGGER);
	}
}