(ns uncomplicate.neanderthal.opencl-test
  (:require [midje.sweet :refer :all]
            [uncomplicate.neanderthal
             [protocols :as p]
             [opencl :refer :all]
             [core :refer :all]
             [real :refer :all]
             [math :refer [pow]]]
            [uncomplicate.clojurecl
             [core :refer [with-default with-release *command-queue*]]])
  (:import [uncomplicate.neanderthal.protocols
            RealVector RealMatrix]))

(def cnt (long (+ 1000  (pow 2 25))))
(def x-magic 2)
(def y-magic 5)

(facts
 "RealVector methods"
 (with-default
   (let [host-x (doto (sv cnt) (fset! x-magic))
         host-y (doto (sv cnt) (fset! y-magic))]
     (with-release [settings (cl-settings *command-queue*)
                    cl-x (cl-sv settings cnt)
                    cl-y (cl-sv settings cnt)]

       (dim cl-x) => cnt

       (fset! cl-x x-magic)
       (fset! cl-y y-magic)
       (fset! host-x 6 100000.0)
       (write! cl-x host-x)

       (float (dot cl-x cl-y)) => (float (dot host-x host-y))

       (float (asum cl-x))
       => (float (+ 100000 (* (double x-magic) (double cnt))))

       (nrm2 cl-x) => (roughly (nrm2 host-x))

       (iamax cl-x) => 6

       (read! (scal! 2 cl-x) (sv cnt)) => (scal! 2 host-x)

       (read! (axpy! cl-y 2 cl-x) (sv cnt))
       => (axpy! host-y 2 host-x)))))


(facts
 "Carrier methods"
 (with-default
   (let [host-x (doto (sv cnt) (fset! 3.5))
         host-y (doto (sv cnt) (fset! 1.1))]
     (with-release [settings (cl-settings *command-queue*)
                    cl-x (cl-sv settings cnt)
                    cl-y (cl-sv settings cnt)]

       (p/byte-size cl-x) => Float/BYTES

       (write! cl-x host-x) => cl-x
       (write! cl-y host-y) => cl-y

       (with-release [cl-zero (zero cl-x)]
         (read! cl-zero (sv cnt))) => (sv cnt)

         (swp! cl-x cl-y) => cl-x
         (swp! cl-x cl-y) => cl-x

         (read! cl-x (sv cnt)) => host-x

         (copy! cl-x cl-y) => cl-y

         (read! cl-y host-y) => host-x))))

(def m-cnt 2048)
(def n-cnt 1024)
(def a-magic 3)
(def x-magic 2)
(def y-magic 5)

(facts
 "Real matrix-vector multiplication."
 (with-default
   (let [host-a (doto (sge m-cnt n-cnt) (fset! a-magic))
         host-x (doto (sv n-cnt) (fset! x-magic))
         host-y (doto (sv m-cnt) (fset! y-magic))]
     (with-release [settings (cl-settings *command-queue*)
                    cl-a (cl-sge settings m-cnt n-cnt)
                    cl-x (cl-sv settings n-cnt)
                    cl-y (cl-sv settings m-cnt)]

       (fset! cl-a a-magic)
       (fset! cl-x x-magic)
       (fset! cl-y y-magic)

       (read! (mv! cl-y 10 cl-a cl-x 100) (sv m-cnt))
       => (mv! host-y 10 host-a host-x 100)))))

(set! *unchecked-math* false)
(def m-cnt 4096)
(def k-cnt 4096)
(def n-cnt 4096)
(def host-a (sge m-cnt k-cnt (range (* m-cnt k-cnt))))
(def host-b (sge k-cnt n-cnt (map (partial * 2) (range (* m-cnt k-cnt)))))
(def host-c (sge m-cnt n-cnt (map (partial * 2) (range (* m-cnt n-cnt)))))
(set! *unchecked-math* :warn-on-boxed)

(facts
 "Real matrix-matrix multiplication."
 (with-default
   (with-release [settings (cl-settings *command-queue*)
                  cl-a (cl-sge settings m-cnt k-cnt)
                  cl-b (cl-sge settings k-cnt n-cnt)
                  cl-c (cl-sge settings m-cnt n-cnt)]

     (write! cl-a host-a)
     (write! cl-b host-b)
     (write! cl-c host-c)

     (time (do (mm! cl-c 10 cl-a cl-b 100) (finish! *command-queue*)))
     ;=> (time (mm! host-c 10 host-a host-b 100))
     )))