package magithub.servlets;

import Engine.*;
import Engine.GsonClasses.TreeComponentsData;
import com.google.gson.Gson;
import constants.Constants;
import magithub.utils.ServletUtils;
import magithub.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;


public class PullRequestServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        MAGitHubManager magithubManager = ServletUtils.getMagitHubManager(getServletContext());
        String activeUsername = SessionUtils.getUsername(request);
        User activeUser = magithubManager.getUser(activeUsername);
        String action = request.getParameter(Constants.PR_ACTION);
        String prId = request.getParameter(Constants.PR_ID);

        Gson gson = new Gson();
        String json = null;

        try {
            switch(action)
            {
                case "approve":
                    magithubManager.handlePullRequest(activeUser, prId, "approve");
                    json = ServletUtils.getJsonResponseString("Pull request approved!", true);
                    break;
                case "decline":
                    magithubManager.handlePullRequest(activeUser, prId, "decline");
                    json = ServletUtils.getJsonResponseString("Pull request declined!", true);
                    break;
                case "send":
                    Repository activeRepository = activeUser.getManager().getActiveRepository();
                    String repositoryName = activeRepository.getName();
                    String targetBranchName = request.getParameter(Constants.PR_TARGET_BRANCH);
                    String baseBranchName = request.getParameter(Constants.PR_BASE_BRANCH);
                    String description = request.getParameter(Constants.PR_DESCRIPTION);

                    String remoteUsername = ServletUtils.getUsernameFromRepositoryPath(activeRepository.getRemotePath());
                    magithubManager.sendPullRequest(activeUsername, remoteUsername, repositoryName, targetBranchName, baseBranchName, description);
                    json = ServletUtils.getJsonResponseString("Pull request sent to: " + remoteUsername, true);
                    break;
                default:
                    json = ServletUtils.getJsonResponseString("unrecognized action requested", false);
            }

        } catch (Exception e) {
            json = ServletUtils.getJsonResponseString(e.getMessage(), false);
        } finally {
            PrintWriter out = response.getWriter();
            out.println(json);
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