package magithub.servlets;

import Engine.Repository;
import com.google.gson.Gson;
import constants.Constants;
import Engine.User;
import Engine.MAGitHubManager;
import magithub.utils.ServletUtils;
import magithub.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class ForkRepositoryServlet extends HttpServlet {
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        MAGitHubManager magitHubManager = ServletUtils.getMagitHubManager(getServletContext());

        String username = SessionUtils.getUsername(request);
        String otherUserName = request.getParameter(Constants.OTHER_USERNAME);
        String otherUserRepositoryName = request.getParameter(Constants.OTHER_USER_REPOSITORY_NAME);
        String message = ServletUtils.getJsonResponseString("\"" + otherUserRepositoryName + "\" repository forked successfully!", true);
        User user = magitHubManager.getUser(username);

        try {
            Repository forkedRepository = user.getManager().fork(otherUserName, otherUserRepositoryName);
            user.addNewRepositoryData(forkedRepository, user.getForkedRepositories());
            magitHubManager.getUser(otherUserName).addForkedRepository(username, otherUserRepositoryName);
        } catch (Exception e) {
            message = ServletUtils.getJsonResponseString(e.getMessage(), false);
        }finally {
            try (PrintWriter out = response.getWriter()) {
                Gson gson = new Gson();
                String json = gson.toJson(message);
                out.println(json);
                out.flush();
            }
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