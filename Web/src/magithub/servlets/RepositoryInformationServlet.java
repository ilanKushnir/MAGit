package magithub.servlets;

import Engine.User;
import com.google.gson.Gson;
import constants.Constants;
import magithub.utils.ServletUtils;
import magithub.utils.SessionUtils;
import Engine.MAGitHubManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static constants.Constants.USERNAME;

public class RepositoryInformationServlet extends HttpServlet {
    private static String WATCH_REPOSITORY_URL = "pages/repository.html";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

//        response.setContentType("application/json");
//        MAGitHubManager userManager = ServletUtils.getMagitHubManager(getServletContext());
//        String currentUserName = SessionUtils.getUsername(request);
//        User currentUser = userManager.getUser(currentUserName);
//        String repositoryName= request.getParameter("repositoryName");
//
//        currentUser.getManager().SwitchRepositoryInHub(repositoryName);
//        request.getSession(true).setAttribute(Constants.CURRENT_REPOSITORY_TO_WATCH, repositoryName);
//        String newUrl = "pages/repositoryInformation/repositoryInformation.html";
//        try (PrintWriter out = response.getWriter()) {
//            Gson gson = new Gson();
//            String json = gson.toJson(newUrl);
//            out.println(json);
//            out.flush();
//        }
    }


// <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
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
