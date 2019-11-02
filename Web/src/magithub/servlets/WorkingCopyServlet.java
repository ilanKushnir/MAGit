package magithub.servlets;

import Engine.Folder;
import Engine.GsonClasses.TreeComponentsData;
import Engine.MAGitHubManager;
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


public class WorkingCopyServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        MAGitHubManager magithubManager = ServletUtils.getMagitHubManager(getServletContext());
        String action = request.getParameter(Constants.WC_ACTION);
        String activeUserName = SessionUtils.getUsername(request);
        User activeUser = magithubManager.getUser(activeUserName);
        Gson gson = new Gson();
        String json = null;

        if (action.equals("refreshWC")) {
            Folder wcTree = activeUser.getManager().buildWorkingCopyTree();
            TreeComponentsData workingCopyData = new TreeComponentsData(wcTree, activeUser.getManager().getActiveRepository().getRootPath());
            json = gson.toJson(workingCopyData);
        }

        PrintWriter out = response.getWriter();
        out.println(json);
        out.flush();
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