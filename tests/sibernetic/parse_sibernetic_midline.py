import sys 
import math

def dist(x1,y1,x2,y2):
    return math.sqrt( (x1-x2)**2 +(y1-y2)**2 )
    
def plot_positions(pos_file_name, rate_to_plot = 1):
    
    postions_file = open(pos_file_name)
    
    max_lines = 10000
    max_time_ms = 100
    
    line_num = 0

    import matplotlib.pyplot as plt

    print("Loading: %s"%pos_file_name)
    
    fig = plt.figure()
    ax = fig.add_subplot(111)
    
    x = {}
    y = {}
    z = {}
    
    num_plotted_frames=0
    points_plotted = 0
    
    middle_points=[]
    ave_points=[]
    middle_point_speed_x = []
    middle_point_speed_y = []
    ave_point_speed_x = []
    ave_point_speed_y = []
    time_points = []
    
    for line in postions_file:

        line_num +=1
        if line_num>max_lines:
            print("Finished parsing file, as max number of lines reached!")
            break
        
        if line_num%rate_to_plot==1:
            
            words = line.split()
            points = (len(words)-4)/3
            middle_point = points/2

            t_ms = float(words[0])

            if t_ms>max_time_ms:
                print("Finished parsing file, as max time reached!")
                break
            x[t_ms] = [float(w) for w in words[2:2+points]]
            y[t_ms] = [float(w) for w in words[3+points:3+2*points]]
            z[t_ms] = [float(w) for w in words[4+2*points:]]

            print("======   L%i: at time: %s found %i points: [(%s,%s,%s),...]"%(line_num,t_ms, points,x[t_ms][0],y[t_ms][0],z[t_ms][0]))

            spacing = 30

            xs = []
            ys = []
            avx = 0
            avy = 0
            offset = num_plotted_frames*spacing
            for i in range(len(x[t_ms])):
                
                # Swap x and y so worm moves "up"
                xs.append(y[t_ms][i]+offset)
                ys.append(x[t_ms][i])
                
                avx+=xs[-1]-offset
                avy+=ys[-1]
                points_plotted+=1
            avx = avx/points
            avy = avy/points
            
                
            if len(middle_points)>0:
                dt = t_ms-time_points[-1]
                middle_point_speed_x.append((xs[middle_point]-offset-middle_points[-1][0])/dt)
                middle_point_speed_y.append((ys[middle_point]-middle_points[-1][1])/dt)
                dav = dist(avx,avy,ave_points[-1][0],ave_points[-1][1])
                ave_point_speed_x.append((avx-ave_points[-1][0])/dt)
                ave_point_speed_y.append((avy-ave_points[-1][1])/dt)
                
                
                print("  Speed of point %i: (%s,%s) -> (%s,%s): x %sum/ms, y %sum/ms"%(middle_point,middle_points[-1][0],middle_points[-1][1],xs[middle_point],ys[middle_point],middle_point_speed_x[-1],middle_point_speed_y[-1]))
                print("  Speed of av point (%s,%s) -> : (%s,%s): x %sum/ms, y %sum/ms"%(ave_points[-1][0],ave_points[-1][1],avx,avy,ave_point_speed_x[-1],ave_point_speed_y[-1]))
                
            middle_points.append((xs[middle_point]-offset,ys[middle_point]))
            ave_points.append((avx,avy))
            time_points.append(t_ms)

            print("  Plotting frame %i at %s ms; line %i: [(%s,%s),...#%i]\n"%(num_plotted_frames,t_ms,line_num,xs[0],ys[0],len(xs)))
            ax.plot(xs,ys,'-')
            num_plotted_frames+=1
            if num_plotted_frames%5 == 1:
                time = '%sms'%t_ms if not t_ms==int(t_ms) else '%sms'%int(t_ms)
                ax.text(50+((num_plotted_frames-1)*spacing), 10, time, fontsize=12)
            
    print("Loaded: %s points from %s, showing %s points in %i frames"%(line_num,pos_file_name,points_plotted,num_plotted_frames))


    fig = plt.figure()
    plt.plot(time_points[1:],middle_point_speed_x,'cyan',label='Speed in x dir of point %i/%i'%(middle_point,points))
    plt.plot(time_points[1:],middle_point_speed_y,'red',label='Speed in y dir of point %i/%i'%(middle_point,points))
    plt.plot(time_points[1:],ave_point_speed_x,'blue',label='Speed in x of average of %i points'%points)
    plt.plot(time_points[1:],ave_point_speed_y,'green',label='Speed in y of average of %i points'%points)
    plt.legend()
    plt.show()

if __name__ == '__main__':
    

    if len(sys.argv) == 2:
        pos_file_name = sys.argv[1]
    else:
        pos_file_name = 'crawling_at_agar.txt'


    plot_positions(pos_file_name,rate_to_plot=20)