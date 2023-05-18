import shutil
import os

# Copy all the .class files in the current directory to the examples

examples_dir_name = "examples"
tranformed_files_dir_name = "jumbotracer-transformed"

if __name__ == "__main__":

    for example in os.listdir(f"../{examples_dir_name}"):
        for classFile in os.listdir("."):
            if classFile.endswith(".class"):
                target_path = os.path.join("..", examples_dir_name, example, tranformed_files_dir_name, classFile)
                open(target_path, "w+").close()  # create file if needed
                shutil.copyfile(classFile, target_path)

