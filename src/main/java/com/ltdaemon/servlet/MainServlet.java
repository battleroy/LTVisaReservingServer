package com.ltdaemon.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;


public class MainServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("Get called.");

        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()));
            bw.write("<html><head><title>LT Visa Monster</title></head><body><h1>Hello</h1></body></html>");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
