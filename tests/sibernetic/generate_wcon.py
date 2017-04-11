import sys 
import math


def dist(x1,y1,x2,y2):
    return math.sqrt( (x1-x2)**2 +(y1-y2)**2 )
    
def generate_wcon(pos_file_name, 
                  wcon_file_name, 
                  rate_to_plot = 1, 
                  max_lines = 1e12, 
                  max_time_s = 1e12,
                  plot = False):
    
    postions_file = open(pos_file_name)
    
    line_num = 0

    print("Loading: %s"%pos_file_name)
    
    ts = []
    x = {}
    y = {}
    z = {}
    
    if plot:
        import matplotlib.pyplot as plt
        fig = plt.figure()
        ax = fig.add_subplot(111)
    
    num_plotted_frames=0
    points_plotted = 0
    
    middle_points=[]
    ave_points=[]
    middle_point_speed_x = []
    middle_point_speed_y = []
    ave_point_speed_x = []
    ave_point_speed_y = []
    time_points = []
    
    num_frames=0
    
    for line in postions_file:

        line_num +=1
        if line_num>max_lines:
            print("Finished parsing file, as max number of lines reached!")
            break
        
        if line_num==1 or line_num%rate_to_plot==0:
            num_frames+=1
            words = line.split()
            points = (len(words)-4)/3
            middle_point = points/2

            t_s = float(words[0])
            ts.append(t_s)

            if t_s>max_time_s:
                print("Finished parsing file, as max time reached!")
                break
            x[t_s] = [float(w) for w in words[2:2+points]]
            y[t_s] = [float(w) for w in words[3+points:3+2*points]]
            z[t_s] = [float(w) for w in words[4+2*points:]]

            print("======   L%i: at time: %s sec found %i points: [(%s,%s,%s),...]"%(line_num,t_s, points,x[t_s][0],y[t_s][0],z[t_s][0]))

            if plot:
                spacing = 30

                xs = []
                ys = []
                avx = 0
                avy = 0
                offset = num_plotted_frames*spacing
                for i in range(len(x[t_s])):

                    # Swap x and y so worm moves "up"
                    xs.append(y[t_s][i]+offset)
                    ys.append(x[t_s][i])

                    avx+=xs[-1]-offset
                    avy+=ys[-1]
                    points_plotted+=1
                avx = avx/points
                avy = avy/points


                if len(middle_points)>0:
                    dt = t_s-time_points[-1]
                    middle_point_speed_x.append((xs[middle_point]-offset-middle_points[-1][0])/dt)
                    middle_point_speed_y.append((ys[middle_point]-middle_points[-1][1])/dt)
                    dav = dist(avx,avy,ave_points[-1][0],ave_points[-1][1])
                    ave_point_speed_x.append((avx-ave_points[-1][0])/dt)
                    ave_point_speed_y.append((avy-ave_points[-1][1])/dt)


                    print("  Speed of point %i: (%s,%s) -> (%s,%s): x %sum/s, y %sum/s"%(middle_point,middle_points[-1][0],middle_points[-1][1],xs[middle_point],ys[middle_point],middle_point_speed_x[-1],middle_point_speed_y[-1]))
                    print("  Speed of av point (%s,%s) -> : (%s,%s): x %sum/s, y %sum/s"%(ave_points[-1][0],ave_points[-1][1],avx,avy,ave_point_speed_x[-1],ave_point_speed_y[-1]))

                middle_points.append((xs[middle_point]-offset,ys[middle_point]))
                ave_points.append((avx,avy))
                time_points.append(t_s)

                print("  Plotting frame %i at %s s; line %i: [(%s,%s),...#%i]\n"%(num_plotted_frames,t_s,line_num,xs[0],ys[0],len(xs)))
                ax.plot(xs,ys,'-')
                num_plotted_frames+=1
                if num_plotted_frames%5 == 1:
                    time = '%ss'%t_s if not t_s==int(t_s) else '%ss'%int(t_s)
                    ax.text(50+((num_plotted_frames-1)*spacing), 10, time, fontsize=12)

    info = "Loaded: %s points from %s, saving %i frames"%(line_num,pos_file_name,num_frames)   
    print(info)
    
    
    wcon = open(wcon_file_name,'w')
    
    wcon.write('''{
    "units":{ "t":"s - check with Andrey!!",     
              "x":"um", 
              "y":"um"},
    "comment":"Saved from Sibernetic data.",
    "note":"%s",
    "data":[\n'''%info)
    
    for t in ts:
        wcon.write('''
           {"id":1, "t":%s,
            "x":%s, 
            "y":%s
	       }'''%(t,x[t],y[t]))
        if t==ts[-1]:
            wcon.write('\n        ]')
        else:
            wcon.write(',\n')

    wcon.write('\n}\n')
    wcon.close()
    postions_file.close()
    
    if plot:
        
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


    generate_wcon(pos_file_name,"sibernetic_test_full.wcon",rate_to_plot=5)
    generate_wcon(pos_file_name,"sibernetic_test_small.wcon",rate_to_plot=20,max_time_s = 20)
    
    generate_wcon(pos_file_name,"none.wcon",rate_to_plot=20, plot = True)