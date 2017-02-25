import sys 


def plot_positions(pos_file_name, rate_to_plot = 1):
    
    postions_file = open(pos_file_name)
    
    max_lines = 1000
    max_time_ms = 100
    
    line_num = 0

    xmin=-1
    xmax=-1
    ymin=-1
    ymax=-1
    zmin=-1
    zmax=-1

    import matplotlib.pyplot as plt

    print("Loading: %s"%pos_file_name)
    
    fig = plt.figure()

    ax = fig.add_subplot(111)
    
    init = False
    
    x = {}
    y = {}
    z = {}
    
    num_plotted_frames=0
    points_plotted = 0
    
    for line in postions_file:

        line_num +=1
        if line_num>max_lines:
            print("Finished parsing file, as max number of lines reached!")
            break
            
        words = line.split()
        points = (len(words)-4)/3
        
        t_ms = float(words[0])
        x[t_ms] = [float(w) for w in words[2:2+points]]
        y[t_ms] = [float(w) for w in words[3+points:3+2*points]]
        z[t_ms] = [float(w) for w in words[4+2*points:]]
        
        print("======   L%i: at time: %s found %i points:\n   %s\n   %s\n   %s"%(line_num,t_ms, points,x[t_ms],y[t_ms],z[t_ms]))
        
        spacing = 30

        xs = []
        ys = []
        for i in range(len(x[t_ms])):
            xs.append(y[t_ms][i]+num_plotted_frames*spacing)
            ys.append(x[t_ms][i])
            points_plotted+=1

        print(" >> Plotting frame %i at %s ms; line %i: [(%s,%s),...#%i]\n"%(num_plotted_frames,t_ms,line_num,xs[0],ys[0],len(xs)))
        ax.plot(xs,ys,'.')
        num_plotted_frames+=1
        if num_plotted_frames%5 == 1:
            time = '%sms'%t_ms if not t_ms==int(t_ms) else '%sms'%int(t_ms)
            ax.text(50+((num_plotted_frames-1)*spacing), 510, time, fontsize=12)
            
    print("Loaded: %s points from %s, showing %s points in %i frames"%(line_num,pos_file_name,points_plotted,num_plotted_frames))

    plt.show()

if __name__ == '__main__':
    

    if len(sys.argv) == 2:
        pos_file_name = sys.argv[1]
    else:
        pos_file_name = 'crawling_at_agar.txt'


    plot_positions(pos_file_name)