package magithub.servlets;

        import Engine.MAGitHubManager;
        import Engine.MergeConflict;
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
        import java.util.LinkedList;


public class PushServlet extends HttpServlet {
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        MAGitHubManager magithubManager = ServletUtils.getMagitHubManager(getServletContext());
        String currentUserName = SessionUtils.getUsername(request);
        User currentUser = magithubManager.getUser(currentUserName);
        String branchToPush = request.getParameter(Constants.BRANCH_TO_PUSH);
        String remoteUsername = request.getParameter(Constants.REMOTE_USERNAME);
        User remoteUser = magithubManager.getUser(remoteUsername);

        Gson gson = new Gson();
        String json = null;

        try {
            currentUser.getManager().pushMagithub(remoteUser);
            remoteUser.setShouldSwitch(true);

            json = ServletUtils.getJsonResponseString(branchToPush + " pushed successfully", true);
        } catch (Exception e) {
            json = ServletUtils.getJsonResponseString(e.getMessage(), false);
        } finally {
            try (PrintWriter out = response.getWriter()) {
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