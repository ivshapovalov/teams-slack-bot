# Teams slack bot

The slack bot preprocess commands from slack chat and constructs requests to the Teams microservice. And also construct successful/unsuccessful responses to the slack chat.

[doc](https://github.com/JujaLabs/docs/tree/master/architecture/teams_slackbot)


**For test slackbot on real remote server with real slack Team
 1. Fill properties file (teams.baseURL,slack.slashCommandToken)
 2. Gradle build
 3. Copy \build\libs\teams-slack-bot.jar to remote server
 4. For use FakeUserRepository (slackNames @a, @b, etc) instead of real UserRepository 
 Run jar file:
 
 java -jar -Dspring.profiles.active=test teams-slack-bot.jar
 5. Create your slack Team
 6. Create your slack app, add slash command (https://api.slack.com/apps)
    for example
    command: /teams-activate
    request url: http://yourserver:port/v1/commands/teams/activate
 7. Test your real slack     
