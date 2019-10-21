package magithub.servlets;

import Engine.MAGitHubManager;
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


public class LoadRepositoryServlet extends HttpServlet {
    private static String LOAD_REPOSITORY_URL = "repository.html";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        MAGitHubManager magithubManager = ServletUtils.getMagitHubManager(getServletContext());
//        String currentUserName = SessionUtils.getUsername(request);     // redundent?
//        User currentUser = magithubManager.getUser(currentUserName);    //  redundent?
        String repositoryName= request.getParameter(Constants.REPOSITORY_NAME);
        request.getSession(true).setAttribute(Constants.CURRENT_WATCHED_REPOSITORY, repositoryName);

        Gson gson = new Gson();
        String json = null;

        try {
            magithubManager.switchActiveRepository(repositoryName);
            json = ServletUtils.getJsonResponseString(LOAD_REPOSITORY_URL, true);
            /// check
            response.sendRedirect(LOAD_REPOSITORY_URL);
        } catch (Exception e) {
            json = ServletUtils.getJsonResponseString(e.getMessage(), false);
            try (PrintWriter out = response.getWriter()) {
                out.println(json);
                out.flush();
            }
        } finally {
//            try (PrintWriter out = response.getWriter()) {
//                out.println(json);
//                out.flush();
//            }

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