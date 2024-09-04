# Project 0: Computing Environment, CMSC424, Fall 2024

Over the course of the semester, you will work with a variety of software packages. Installing those packages and getting started can often be a hassle, because of software dependencies. You have three choices.

* (**Preferred Option**) Use Docker (as discussed below). If you have a reasonably modern machine (within last 5-6 years), Docker should generally work fine, but with older laptops, the performance may not be as good. See below for more details on this.
* Install the different software packages on your own machine (most of these packages should have tutorials to install them on different OSs). If you have a Linux box or a Mac, this should be possible; it will be more difficult with Windows. In any case, although TAs would try their best, they would likely not be able to help you with any problems.
* Use a VM in the CS Department's Horvitz Cluster. Last year, only one person chose to use this, so we will likely do this on demand, if anyone is really interested. You can also similarly use a VM in Amazon or Microsoft Azure clusters -- both of them provide free VMs for beginning users. The problem with this approach is that you only have `ssh` access into that machine, so you can't run a web browser, etc., without some work.

---

### Git & Github

Git is one of the most widely used version control management systems today, and invaluable when working in a team. GitHub is a web-based hosting service built around git --
it supports hosting git repositories, user management, etc. There are other similar services, e.g., bitbucket.

We will use GitHub to distribute the assignments, and other class materials. Our use of git/github for the class will be minimal.

#### Clone the Class Repository

You don't need a GitHub account to clone the class repository. Simply install git (on Windows, http://gitforwindows.org/ is helpful) and then run:

`git clone https://github.com/umd-db/cmsc424-fall2024.git`

You can do `git pull` (from within the `cmsc424-fall2024` directory) to fetch the newly added material.

*NOTE*: If you are having trouble installing `git`, you can just download the files instead (as a zipfile), although updating may become tedious.

---

### Docker

Docker is a containerization platform that is used to package your application and all its dependencies together in the form of containers to make sure that your application works seamlessly in any environment which can be developed or tested or in production. We will provide a `Dockerfile` for different assignments, that will help you start containers with the requisite packages installed.

- In order to use this option, you have to first install Docker engine on your host machine. See the instructions on the [Docker website](https://docs.docker.com/engine/install/). (Docker Desktop for Windows requires either WSL 2 or Hyper-V. We recommend [using WSL 2](https://learn.microsoft.com/en-us/windows/wsl/install) as it is less likely to cause trouble and is available in all Windows editions)

- Docker makes things **super-easy**. We will provide you with appropriate setup files -- all you need to do is `docker build` and `docker run` to start the container. Specifically:
   - In the git sub-directory `project0`, run `docker build -t "cmsc424-project0" .` (NOTE: the dot in the end is important). This will build the image of the container you need to run.
   - Run the docker image: `docker run --rm -ti -p 8888:8888 -p 5432:5432 cmsc424-project0:latest`. This will start the container and load data into the postgres instance. 
   - At this point, you will be in the docker container in the `/home/project0` directory. 
- Some other docker commands that you would need to be familiar with:
   - `docker stop`:  Stops the running container. 
   - `docker restart`: Restarts the container.
   - `docker cp`: Copies files between the container and the host. 
   - `docker rm`: Removes the docker container. 

---

### PostgreSQL

PostgreSQL is a full-fledged and powerful relational database system, and will be used for several assignments. 

PostgreSQL is already installed on your container. To get started, start the container using `docker run --rm -ti -p 8888:8888 -p 5432:5432 cmsc424-project0:latest`. 
The current version of PostgreSQL is 15.1. However, the version installed on the containers is 14.6, the one available through `apt-get`. You will find the detailed documentation at: https://www.postgresql.org/docs/14/index.html. 

Following steps will get you started with creating a database and populating it with the `University` dataset provided on the book website: http://www.db-book.com 

1. You will be using PostgreSQL in the client-server mode. Recall that the server is a continuously running process that listens on a specific port (the default port for PostgreSQL is 5432). The client and server are often on different machines, but for you, it may be easiest if they are on the same machine (i.e., the Docker container). 

   Important: The server should be already started on your container -- you do not need to start it. However, the following two help pages discuss how to start the
   server: [Creating a database cluster](http://www.postgresql.org/docs/current/static/creating-cluster.html) and [Starting the server](http://www.postgresql.org/docs/current/static/server-start.html)

2. PostgreSQL server has a default user with admin privilleges called **postgres**. By default, PostgreSQL uses [peer authentication](https://www.postgresql.org/docs/current/auth-peer.html), which requires you to use the same username for your operating system as the PostgreSQL username in order to log into PostgreSQL under that username. The following command temporarily changes the OS user to `postgres` (you are initially logged in as `root` in the Docker container), which allows you to log into PostgreSQL as the `postgres` user, then creates a new PostgreSQL user called `root` and grants it the ability to create new databases (`-d`).
   ```
   sudo -u postgres createuser -d root
   ```

3. Next, let's create a new database called **university**. Note that you don't need to prefix the command with `sudo -u postgres` anymore because you're already logged in as `root` in your OS and there is a `root` user just created above in PostgreSQL.
	```
	createdb university
	```
4. Now we can connect to this new database via a PostgreSQL client called `psql`. Make sure you run the following command from the `/home/project0/` directory for the later commands to work.
	```
	psql university
	```
	`psql` can take more options: you can specify a different user, a different port, another host name etc. See documentation: http://www.postgresql.org/docs/current/static/app-psql.html.

5. The `psql` program has a number of internal commands that are not SQL commands; such commands are often client and database specific. For `psql`, they begin with the
   backslash character: `\`. For example, you can get help on the syntax of various PostgreSQL SQL commands by typing: `\h`.

   All commands like this can be found at:  http://www.postgresql.org/docs/current/static/app-psql.html. `\?` will also list them out.

   The `\d` is used to list the tables in the database. Initially, there is no table in the database.
   To populate the database using the provided university dataset, run the following commands
   ```
   \i DDL.sql 
   ```

   followed by
   ```
   \i smallRelationsInsertFile.sql
   ```

   Run `\d` again and you should see the new tables.

6. Use `\password` then follow the instructions and set `root` as the password of the user `root`. This is so that we can connect to PostgreSQL with other clients (e.g. JDBC or psycopg).

   NOTE: It is a bad security practice to use such a password or put the password out in the open (e.g. in a Jupyter notebook), but we do that here to keep things simple.

7. Run `\q` to exit `psql`.

8. Repeat steps 3-5 to create a different database ```university_large``` for the larger dataset `largeRelationsInsertFile.sql` (It will take a considerably longer time to import this file). You will need this for the reading homework.

---

### Python and Jupyter/IPython

We will be using Python for most of the assignments; you wouldn't typically use Python for systems development, but it works much better as an instructional tool. Python is easy to pick up, and we will also provide skeleton code for most of the assignments. 

IPython is an enhanced command shell for Python, that offers enhanced introspection, rich media, additional shell syntax, tab completion, and rich history. 

**IPython Notebook** started as a web browser-based interface to IPython, and proved especially popular with Data Scientists. A few years ago, the Notebook functionality was forked off as a separate project, called [Jupyter](http://jupyter.org/). Jupyter provides support for many other languages in addition to Python. 

* If you're not already in the docker container, start it with `docker run --rm -ti -p 8888:8888 -p 5432:5432 cmsc424-project0:latest`. Python, IPython, and Jupyter are already loaded.

* To use Jupyter Notebook, do `cd /home/project0` followed by: 
	```
	jupyter-notebook --port=8888 --allow-root --no-browser --ip=0.0.0.0
	``` 
This will start a server inside the Docker container, listening on port 8888. We will access it from the host machine (as discussed above, the Dockerfile maps the 8888 port on the guest container to the 8888 port on the host). To do that, copy and paste the whole URL that starts with `http://127.0.0.1:8888` from the output of the above command into your browser.

* You should see the notebooks in the `/home/project0/` directory. Click to open the "Jupyter-Getting-Started" notebook, and follow the instruction therein.

* Other notebooks cover the basics of SQL and run SQL queries on the PostgreSQL instance above. 

---

### Submit the assignment

Submit the assignment through ELMS. In the `university` database, run the following query in PostgreSQL:
	```
	select count(*) from student;
	``` 
Copy the result (just the number --- not the rest of the output) into the submission page for Assignment 0 on ELMS, and you are done!

---
