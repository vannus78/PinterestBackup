# PinterestBackup
Backup user's PIN from pinterest into local directory.</br>
</br>
Simple Command line program to backup a pinterest user PINS to local disk.</br>

<h2>Usage</h2>
java -jar PinterestBackup.jar Username DestinationPath [-s] [-v]</br>
</br>
Username:          Pinterest user to backup</br>
DestinationPath:   Path to store backup images</br>
-s:                Syncronize local images with account. WARNING this option delete local files if the images are not pinned anymore.</br>
-v:                Verbose mode off.

<h2>Dependencies</h2>
<ul>
<li>JDK 1.8</li>
<li>gson 2.6.2 - Google library used to store JSON information into java classes</li>
<li>httpclient-4.5.2 - Apache http client, used to make AJAX calls</li>
<li>httpcore-4.4.4 - Apache http core, supports for http client</li>
<li>httpcore-4.4.4 - Apache http core, supports for http client</li>
<li><a href="https://github.com/vannus78/PinterestWebLibrary">PinterestWebLibrary</a> - Library to retrieve Pinterest boards and pins</li>
</ul>
