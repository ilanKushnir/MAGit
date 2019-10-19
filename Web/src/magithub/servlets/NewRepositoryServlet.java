package magithub.servlets;

import Engine.GsonClasses.RepositoryData;
import Engine.MAGitHubManager;
import Engine.Repository;
import com.google.gson.Gson;
import Engine.User;
import magithub.utils.ServletUtils;
import magithub.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class NewRepositoryServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fileContent = request.getParameter("file");
        Gson gson = new Gson();
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        MAGitHubManager magitHubManager = ServletUtils.getMagitHubManager(getServletContext());
        String currentUserName = SessionUtils.getUsername(request);
        User user = magitHubManager.getUser(currentUserName);

        try {
            user.getManager().importFromXMLToHub(fileContent);
            out.println(gson.toJson(ServletUtils.getJsonResponseString("Repository created successfully", true)));
            Repository repositoryObj = user.getManager().getActiveRepository();
            String repositoryName = repositoryObj.getName();
            user.addNewRepositoryData(repositoryName);
        } catch (Exception e) {
            out.println(gson.toJson(ServletUtils.getJsonResponseString(e.getMessage(), false)));

        }
    }

}
