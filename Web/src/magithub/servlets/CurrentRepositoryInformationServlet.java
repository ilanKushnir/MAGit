package magithub.servlets;

import Engine.Commit;
import Engine.GsonClasses.CommitData;
import Engine.GsonClasses.RepositoryData;
import Engine.GsonClasses.UserData;
import Engine.MAGitHubManager;
import Engine.Repository;
import Engine.User;
import com.google.gson.Gson;
import magithub.utils.ServletUtils;
import magithub.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.List;


public class CurrentRepositoryInformationServlet extends HttpServlet {
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        MAGitHubManager magitHubManager = ServletUtils.getMagitHubManager(getServletContext());
        String repositoryOwnerName = SessionUtils.getUsername(request);
        User repositoryOwner = magitHubManager.getUser(repositoryOwnerName);
        String currentRepositoryName = SessionUtils.getWatchedRepository(request);

        try (PrintWriter out = response.getWriter()) {
            RepositoryData currentRepositoryData = new RepositoryData(repositoryOwner.getManager().getActiveRepository(), repositoryOwner.getForkedRepositories());
            Gson gson = new Gson();
            String json = gson.toJson(currentRepositoryData);
            out.println(json);
            out.flush();
        } catch (ParseException e) {
            // TODO handle error
            e.printStackTrace();
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
