package com.hand.api.filter;

import org.springframework.core.annotation.Order;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;
@Order(1)
@WebFilter(filterName = "testFilter",urlPatterns = "/*")
public class TestFilter implements Filter
{

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        System.out.println("TestFilter");
        chain.doFilter(request,response);
    }

    @Override
    public void destroy() {

    }
}
