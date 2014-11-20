import sys, getopt, os

def main(argv):

    #Assign defaults
    gridx = 100
    gridy = 100
    window = 5

    help = argv[0] + " -x <gridx> -y <gridy> -w <window>"

    try:
        opts, args = getopt.getopt(argv[1:], "hx:y:w:", ["gridx=", "gridy=", "window="])
    except getopt.GetoptError:
        print help
        sys.exit(2)

    for opt, arg in opts:
        if opt == "-h":
            print help
            sys.exit()
        elif opt in ("-x", "--gridx"):
            gridx = arg
        elif opt in ("-y", "--gridy"):
            gridy = arg
        elif opt in ("-w", "--window"):
            window = arg

    with open('param.scala', 'w') as param:
        param.write('package object param {\n')
        param.write('  val gridx = ' + str(gridx) + '\n')
        param.write('  val gridy = ' + str(gridy) + '\n')
        param.write('  val window = ' + str(window) + '\n')
        param.write('}')

    os.system('sbt \"run Stencil --backend v\"')

    os.remove('param.scala')


if __name__ == "__main__":
    main(sys.argv)