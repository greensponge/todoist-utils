# General
This project is written in Clojure, so if you want to build an uberjar you need to have Clojure installed on your system. 

Use your package manager (Linux), Homebrew (Mac), or Scoop/Chocolatey/Powershell (Windows) to install Clojure on your system.

# My Setup
I have a simple structure in my Todoist, I don't create a new project in Todoist when I have a new project, that is too messy.

Instead, I have projects that look like this, which are kind of self explanatory:

* This Week
* Next Week
* This Month
* Next Month
* Later

In addition to this I have three filters to help me stay organized:
* Today's Objectives (what I must do today)
* Today's Focus (what I must do and what I should do today)
* Tomorrow's Focus (what I must/should do tomorrow + overdue)

This is basically the same setup used by [Carl Pullein](https://www.youtube.com/watch?v=p4U5pK_gsQU) in his Todoist walk-through video, just slightly simplified.

The reason I wanted automation is that manually having to "go through your tasks once a week" is a huge hassle, who has the time? Not me.
I want my tasks to automatically move between the projects (this week, next week... wherever) based on their due date.

I don't want to think about assigning which of these projects a task should go through when I quickly add something to the inbox either.
It should do that automatically based on the due date. And if I fail to complete a task, I can just change the duedate and let this automation move it to where it needs to go.

Additionally, as an example, when it switches months I want the tasks in "Next Month" to go to "This Month", when it's "Next Week" I want it to go there, and so on.
I *never* want to do this manually.

The code in this project is really simple, it just classifies the date of a task (it's :next-month or :this-month, etc.) and based on that a task goes to whatever project id is 
in the `config.edn` file for the corresponding project.

If you want to "watch" more projects and automate tasks there it's simple, just add another row to main:

```clj
(move-task-according-to-date some-project-id)
```

That's it.

# Config

You must add a `config.edn` file to your project root with the following key/values for the program to work as-is:

```
{:api-token "your-api-token"  
:this-week-project-id "your-this-week-project-id" 
:next-week-project-id "your-next-week-project-id"
:this-month-project-id "your-this-month-project-id"
:next-month-project-id "your-next-month-project-id"
:inbox-project-id "your-inbox-project-id"
:later-project-id "your-later-project-id"}
```

The `config.edn` file is ignored in `.gitignore` to avoid accidental leaking.

# Deploying
To deploy this application I just put the uberjar along with the `config.edn` on my server, your use-case may vary. 
You should be able to stick this into a docker container or some other workflow if you wish.

```bash
# Spits out a uberjar in your project root
clj -T:build uberjar

# When you have the jar file somewhere, just execute it whenever you need to, I use cron.
# NOTE: You must have the `config.edn` file together with the uberjar, or it won't work as-is.
java -jar todoist-utils.jar
```
The uberjar function is defined in `build.clj`.

# Cron Schedule
I use cron to execute the uberjar on regular schedule, in this case every 5 minutes:
```bash
*/5 * * * * java -jar /home/greensponge/todoist-utils.jar >> /home/greensponge/todoist-utils.log 2>&1
```
# Roadmap
When I become bothered by something in my Todoist workflow in the future, I plan to extend these utils. If you have any cool automation ideas you can open an issue or create a PR.
