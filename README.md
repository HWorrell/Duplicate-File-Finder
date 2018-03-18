# Duplicate-File-Finder
Program to identify duplicate files on a hard drive

This is a work in progress program to identify files that are duplicates in content in different folders within a hard drive, or between two different drives. It calculates the SHA1 hash of each file in parallel, and uses those hashes to find any occurance of duplication.

Background: I have quite a few backup hard drives. I currently have 4 main drives. An 8TB external as a main backup, a 2TB internal for programs, a 2TB external for larger media files, and a 1TB external for smaller media files and documents. The organization isn't extremely strict, as they are also the product of me upgrading when needed. However, due me not paying a lot of attention when I was changing drives, there is data duplication between some or all of these drives. I don't think it is a major cause for concern, but it is an annoyance that I would like to deal with.

Project Scope: I decided to write a program that would identify duplicate files based on content. The reason for this is that I am not always consistent in my naming practices, and so a simple file name search will likely miss some files. The program will identify all duplicates within my drives, and output a list of files to a text file, so that I can work through the duplicates as needed. As my home computer is a Windows machine, it currently is made for primarily Windows Filesystems. This will change eventually.

Language Choice: I chose to work in Java because I needed more practice working with Filesystems in Java.  I will likely create a version in C/C++ eventually.

Initial Design:  The program begins with a prompt for the drive(s) to be searched.  It can also accept a narrower scope, so that folders can be specified.  The program then uses recursion to find all non-directory files, and stores the path to them in a list.  Once all files have been located, it begins the comparison process.

Comparison: Initially, I used a library to simply compare files.  It would compare sizes, and if the sizes were identical, it would begin comparing the actual contents byte for byte until a difference was found.  This worked fine at smaller scale.  However, because each file needs to be compared to each other file, the running time is O(n^2), and that quickly became a large issue when faced with the 10,000+ files I was working with on a small drive.  As a mitigation measure, I included the ability to ignore files over a certain size, but this is sometimes not an option.

I began brainstorming a way to get around the problems in this process, and eventually hit on the idea of hashing the files.  Hashing the files takes a long time when compared to just comparing all the files directly, but it only has to be done once.  I hash the files, and store the hashes in a Vector along with the original, so that it is simple to determine which hash goes with which file.  I then make a copy of the Vector containing the hashes, and sort it.  Once sorted, it only requires a single pass through the vector to determine which hashes are present more than once, comparing Vector.elementAt(i) and Vector.elementAt(i + 1).

All duplicate hashes are put into a set, so that no matter how many times it is duplicated, it is only present once.  After all duplicates are stored in the set, for each of the duplicate hashes found, it makes a single pass though all of the original hashes, and on finding a match, puts it into the file, which is then written to the hard disk on close.

The program does the file discovery and hash matching in serial, as these are not extremely resource dependent and are very prone to race conditions.  The actual hashing itself, however, is done in parallel, to speed an already slow process.  The n threads all run in a strided manner, to try and spread the work as evenly as possible.  The program is structured to try and prevent the use of syncronization as much as possible, as the threads never access any resources that the others will.

Future plans:

Support OSX/Linux
Find a hashing algorithm that is ideal with regards to speed vs collisions
