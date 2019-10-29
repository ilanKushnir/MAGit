package magithub.servlets;

import Engine.MAGitHubManager;
import Engine.NotificationsCenter;
import Engine.Repository;
import Engine.User;
import com.google.gson.Gson;
import constants.Constants;
import magithub.utils.ServletUtils;
import magithub.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;


public class NotificationsServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        MAGitHubManager magithubManager = ServletUtils.getMagitHubManager(getServletContext());
        String activeUsername = SessionUtils.getUsername(request);
        User activeUser = magithubManager.getUser(activeUsername);

        String action = request.getParameter(Constants.NOTIFICATIONS_ACTION);
        Gson gson = new Gson();
        String notificationsJson = null;
        NotificationsCenter notificationsCenter = activeUser.getNotificationsCenter();
        LinkedList notificationsDataList;


        try {
            switch(action) {
                case "get":
                    notificationsJson = gson.toJson(notificationsCenter);
                    break;
                case "see":
                    notificationsCenter.seenAll();
                    break;
            }
        } catch (Exception e) {
            notificationsJson = ServletUtils.getJsonResponseString(e.getMessage(), false);
        } finally {
            PrintWriter out = response.getWriter();
            out.println(notificationsJson);
            out.flush();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}